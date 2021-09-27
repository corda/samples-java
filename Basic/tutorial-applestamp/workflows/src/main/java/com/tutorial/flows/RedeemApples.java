package com.tutorial.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.tutorial.contracts.AppleStampContract;
import com.tutorial.contracts.BasketOfAppleContract;
import com.tutorial.states.AppleStamp;
import com.tutorial.states.BasketOfApple;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.UUID;

public class RedeemApples {

    @InitiatingFlow
    @StartableByRPC
    public static class RedeemApplesInitiator extends FlowLogic<SignedTransaction>{

        private Party buyer;
        private UniqueIdentifier stampId;

        public RedeemApplesInitiator(Party buyer, UniqueIdentifier stampId) {
            this.buyer = buyer;
            this.stampId = stampId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            /* Obtain a reference to a notary we wish to use.
             * METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            //final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

            //Query the AppleStamp
            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria()
                    .withUuid(Arrays.asList(UUID.fromString(stampId.toString())))
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef appleStampStateAndRef = getServiceHub().getVaultService().queryBy(AppleStamp.class, inputCriteria).getStates().get(0);

            //Query output basketOfApple
            QueryCriteria outputCriteria = new QueryCriteria.VaultQueryCriteria()
                    .withStatus(Vault.StateStatus.UNCONSUMED)
                    .withRelevancyStatus(Vault.RelevancyStatus.RELEVANT);
            StateAndRef basketOfAppleStateAndRef = getServiceHub().getVaultService().queryBy(BasketOfApple.class, outputCriteria).getStates().get(0);
            BasketOfApple originalBasketOfApple = (BasketOfApple) basketOfAppleStateAndRef.getState().getData();

            //Modify output to address the owner change
            BasketOfApple output = originalBasketOfApple.changeOwner(buyer);

            //Build Transaction
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(appleStampStateAndRef)
                    .addInputState(basketOfAppleStateAndRef)
                    .addOutputState(output, BasketOfAppleContract.ID)
                    .addCommand(new BasketOfAppleContract.Commands.Redeem(),
                            Arrays.asList(getOurIdentity().getOwningKey(),this.buyer.getOwningKey()));

            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(buyer);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession)));

            // Notarise and record the transaction in both parties' vaults.
            SignedTransaction result = subFlow(new FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)));

            return result;
        }
    }

    @InitiatedBy(RedeemApplesInitiator.class)
    public static class RedeemApplesResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        public RedeemApplesResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                }
            });

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }

}
//flow start RedeemApplesInitiator buyer: Peter, stampId: