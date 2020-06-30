package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.confidential.IdentitySyncFlow;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import net.corda.finance.workflows.asset.CashUtils;
import net.corda.samples.contracts.IOUContract;
import net.corda.samples.states.IOUState;

import java.lang.IllegalArgumentException;
import java.security.PublicKey;
import java.util.*;

import static net.corda.finance.workflows.GetBalances.getCashBalance;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

public class IOUSettleFlow {

    /**
     * This is the flow which handles the settlement (partial or complete) of existing IOUs on the ledger.
     * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
     * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
     * The flow returns the [SignedTransaction] that was committed to the ledger.
     */
    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier stateLinearId;
        private final Amount<Currency> amount;

        public InitiatorFlow(UniqueIdentifier stateLinearId, Amount<Currency> amount) {
            this.stateLinearId = stateLinearId;
            this.amount = amount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // 1. Retrieve the IOU State from the vault using LinearStateQueryCriteria
            List<UUID> listOfLinearIds = Arrays.asList(stateLinearId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            StateAndRef inputStateAndRefToSettle = (StateAndRef) results.getStates().get(0);
            IOUState inputStateToSettle = (IOUState) ((StateAndRef) results.getStates().get(0)).getState().getData();
            Party counterparty = inputStateToSettle.lender;

            // Step 2. Check the party running this flow is the borrower.
            if (!inputStateToSettle.borrower.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                throw new IllegalArgumentException("The borrower must issue the flow");
            }
            // Step 3. Create a transaction builder.

            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

            TransactionBuilder tb = new TransactionBuilder(notary);

            // Step 4. Check we have enough cash to settle the requested amount.
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), (Currency) amount.getToken());
            if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new IllegalArgumentException("Borrower doesn't have enough cash to settle with the amount specified.");
            } else if (amount.getQuantity() > (inputStateToSettle.amount.getQuantity() - inputStateToSettle.paid.getQuantity())) {
                throw new IllegalArgumentException("Borrow tried to settle with more than was required for the obligation.");
            }

            // Step 5. Get some cash from the vault and add a spend to our transaction builder.
            // Vault might contain states "owned" by anonymous parties. This is one of techniques to anonymize transactions
            // generateSpend returns all public keys which have to be used to sign transaction
            List<PublicKey> keyList = CashUtils.generateSpend(getServiceHub(), tb, amount, getOurIdentityAndCert(), counterparty).getSecond();

            // Step 6. Add the IOU input state and settle command to the transaction builder.

            Command<IOUContract.Commands.Settle> command = new Command<>(
                    new IOUContract.Commands.Settle(),
                    Arrays.asList(counterparty.getOwningKey(),getOurIdentity().getOwningKey())
            );
            tb.addCommand(command);
            tb.addInputState(inputStateAndRefToSettle);

            // Step 7. Only add an output IOU state of the IOU has not been fully settled.
            if (amount.getQuantity() < inputStateToSettle.amount.getQuantity()) {
                tb.addOutputState(inputStateToSettle.pay(amount), IOUContract.IOU_CONTRACT_ID);
            }

            // Step 8. Verify and sign the transaction.
            tb.verify(getServiceHub());
            keyList.addAll(Arrays.asList(getOurIdentity().getOwningKey()));
            SignedTransaction ptx = getServiceHub().signInitialTransaction(tb, keyList);

            // 11. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            FlowSession session = initiateFlow(counterparty);
            new IdentitySyncFlow.Send(session, ptx.getTx());

            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(session)));

            /* 12. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             */
            return subFlow(new FinalityFlow(fullySignedTransaction, session));

        }

    }

    /**
     * This is the flow which signs IOU settlements.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(IOUSettleFlow.InitiatorFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;
        private SecureHash txWeJustSignedId;

        public Responder(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    // Once the transaction has verified, initialize txWeJustSignedID variable.
                    txWeJustSignedId = stx.getId();
                }
            }

            // Create a sign transaction flow
            SignTxFlow signTxFlow = new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker());

            // Run the sign transaction flow to sign the transaction
            subFlow(signTxFlow);

            // Run the ReceiveFinalityFlow to finalize the transaction and persist it to the vault.
            return subFlow(new ReceiveFinalityFlow(otherPartyFlow, txWeJustSignedId));

        }
    }

    /**
     * Self issues the calling node an amount of cash in the desired currency.
     * Only used for demo/sample/training purposes!
     */

    @InitiatingFlow
    @StartableByRPC
    public static class SelfIssueCashFlow extends FlowLogic<Cash.State> {

        Amount<Currency> amount;

        SelfIssueCashFlow(Amount<Currency> amount) {
            this.amount = amount;
        }

        @Suspendable
        @Override
        public Cash.State call() throws FlowException {
            // Create the cash issue command.
            OpaqueBytes issueRef = OpaqueBytes.of(new byte[0]);

            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

            // Create the cash issuance transaction.
            AbstractCashFlow.Result cashIssueTransaction = subFlow(new CashIssueFlow(amount, issueRef, notary));
            return (Cash.State) cashIssueTransaction.getStx().getTx().getOutput(0);
        }

    }

}