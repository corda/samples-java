package net.corda.samples.multioutput.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.samples.multioutput.contracts.SubCountContract;
import net.corda.samples.multioutput.states.OmniCountState;
import net.corda.samples.multioutput.states.SubCountState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SendTwoOutputFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class SendTwoOutputFlowInitiator extends FlowLogic<SignedTransaction> {

        //private variables
        private Party borrowerMe ;
        private Party receiver;
        private Integer amount;


        //public constructor
        public SendTwoOutputFlowInitiator(Party receiver, Integer amount) {

            this.receiver = receiver;
            this.amount = amount;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //Hello World message
            this.borrowerMe = getOurIdentity();

            // Step 1. Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));

            //Compose the State that carries the Hello World message
            final SubCountState output = new SubCountState(amount,borrowerMe,receiver);
            final OmniCountState omniCount = new OmniCountState(amount,borrowerMe,receiver,new UniqueIdentifier());

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
            builder.addOutputState(output);
            builder.addOutputState(omniCount);
            builder.addCommand(new SubCountContract.Commands.Send(),
                    Arrays.asList(this.borrowerMe.getOwningKey(),this.receiver.getOwningKey()) );


            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            // Step 6. Collect the other party's signature using the SignTransactionFlow.
            List<Party> otherParties = output.getParticipants().stream().map(el -> (Party)el).collect(Collectors.toList());
            otherParties.remove(getOurIdentity());
            List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, sessions));
        }
    }

    @InitiatedBy(SendTwoOutputFlowInitiator.class)
    public static class SendTwoOutputFlowResponder extends FlowLogic<Void>{
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public SendTwoOutputFlowResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    /*
                     * SignTransactionFlow will automatically verify the transaction and its signatures before signing it.
                     * However, just because a transaction is contractually valid doesn’t mean we necessarily want to sign.
                     * What if we don’t want to deal with the counterparty in question, or the value is too high,
                     * or we’re not happy with the transaction’s structure? checkTransaction
                     * allows us to define these additional checks. If any of these conditions are not met,
                     * we will not sign the transaction - even if the transaction and its signatures are contractually valid.
                     * ----------
                     * For this hello-world cordapp, we will not implement any aditional checks.
                     * */
                }
            });
            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
            return null;
        }
    }

}
