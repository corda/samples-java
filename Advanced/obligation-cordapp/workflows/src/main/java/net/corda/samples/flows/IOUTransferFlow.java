package net.corda.samples.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.contracts.IOUContract.Commands.Transfer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.contracts.IOUContract;
import net.corda.samples.states.IOUState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;


/**
 * This is the flow which handles transfers of existing IOUs on the ledger.
 * Gathering the counterparty's signature is handled by the [CollectSignaturesFlow].
 * Notarisation (if required) and commitment to the ledger is handled by the [FinalityFlow].
 * The flow returns the [SignedTransaction] that was committed to the ledger.
 */

public class IOUTransferFlow{

    @InitiatingFlow
    @StartableByRPC
    public static class InitiatorFlow extends FlowLogic<SignedTransaction> {
        private final UniqueIdentifier stateLinearId;
        private final Party newLender;

        public InitiatorFlow(UniqueIdentifier stateLinearId, Party newLender) {
            this.stateLinearId = stateLinearId;
            this.newLender = newLender;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // 1. Retrieve the IOU State from the vault using LinearStateQueryCriteria
            List<UUID> listOfLinearIds = new ArrayList<>();
            listOfLinearIds.add(stateLinearId.getId());
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);

            // 2. Get a reference to the inputState data that we are going to settle.
            Vault.Page results = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);
            StateAndRef inputStateAndRefToTransfer = (StateAndRef) results.getStates().get(0);
            IOUState inputStateToTransfer = (IOUState) inputStateAndRefToTransfer.getState().getData();

            // 3. We should now get some of the components required for to execute the transaction
            // Here we get a reference to the default notary and instantiate a transaction builder.

            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

            TransactionBuilder tb = new TransactionBuilder(notary);

            // 4. Construct a transfer command to be added to the transaction.
            List<PublicKey> listOfRequiredSigners = inputStateToTransfer.getParticipants()
                    .stream().map(AbstractParty::getOwningKey)
                    .collect(Collectors.toList());
            listOfRequiredSigners.add(newLender.getOwningKey());

            Command<Transfer> command = new Command<>(
                    new Transfer(),
                    listOfRequiredSigners
            );

            // 5. Add the command to the transaction using the TransactionBuilder.
            tb.addCommand(command);

            // 6. Add input and output states to flow using the TransactionBuilder.
            tb.addInputState(inputStateAndRefToTransfer);
            tb.addOutputState(inputStateToTransfer.withNewLender(newLender), IOUContract.IOU_CONTRACT_ID);

            // 7. Ensure that this flow is being executed by the current lender.
            if (!inputStateToTransfer.lender.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                throw new IllegalArgumentException("This flow must be run by the current lender.");
            }

            // 8. Verify and sign the transaction
            tb.verify(getServiceHub());
            SignedTransaction partiallySignedTransaction = getServiceHub().signInitialTransaction(tb);

            // 9. Collect all of the required signatures from other Corda nodes using the CollectSignaturesFlow
            List<FlowSession> sessions = new ArrayList<>();

            for (AbstractParty participant: inputStateToTransfer.getParticipants()) {
                Party partyToInitiateFlow = (Party) participant;
                if (!partyToInitiateFlow.getOwningKey().equals(getOurIdentity().getOwningKey())) {
                    sessions.add(initiateFlow(partyToInitiateFlow));
                }
            }
            sessions.add(initiateFlow(newLender));
            SignedTransaction fullySignedTransaction = subFlow(new CollectSignaturesFlow(partiallySignedTransaction, sessions));
            /* 10. Return the output of the FinalityFlow which sends the transaction to the notary for verification
             *     and the causes it to be persisted to the vault of appropriate nodes.
             */
            return subFlow(new FinalityFlow(fullySignedTransaction, sessions));
        }
    }


    /**
     * This is the flow which signs IOU settlements.
     * The signing is handled by the [SignTransactionFlow].
     */
    @InitiatedBy(IOUTransferFlow.InitiatorFlow.class)
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
                @NotNull
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an IOU transaction", output instanceof IOUState);
                        return null;
                    });
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

}