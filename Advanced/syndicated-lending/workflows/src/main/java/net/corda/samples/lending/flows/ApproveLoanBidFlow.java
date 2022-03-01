package net.corda.samples.lending.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StaticPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.lending.contracts.LoanBidContract;
import net.corda.samples.lending.states.LoanBidState;
import net.corda.samples.lending.states.ProjectState;

import java.util.Arrays;
import java.util.List;

public class ApproveLoanBidFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier bidIdentifier;

        //public constructor
        public Initiator(UniqueIdentifier bidIdentifier) {
            this.bidIdentifier = bidIdentifier;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            List<StateAndRef<LoanBidState>> loanBidStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(LoanBidState.class).getStates();

            StateAndRef<LoanBidState> inputStateAndRef = loanBidStateAndRefs.stream().filter(loanBidStateAndRef -> {
                LoanBidState loanBidState = loanBidStateAndRef.getState().getData();
                return loanBidState.getLinearId().equals(bidIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Loan Bid Not Found"));

            LoanBidState inputState = inputStateAndRef.getState().getData();

            final LoanBidState output = new LoanBidState(
                    new StaticPointer<>(inputStateAndRef.getRef(), ProjectState.class),
                    inputState.getLinearId(), inputState.getLender(), inputState.getBorrower(),
                    inputState.getLoanAmount(), inputState.getTenure(), inputState.getRateofInterest(),
                    inputState.getTransactionFees(), "APPROVED"
            );

            Party notary = inputStateAndRef.getState().getNotary();

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the inputs and outputs, as well as a command to the transaction builder.
            builder.addInputState(inputStateAndRef);
            builder.addOutputState(output);
            builder.addCommand(new LoanBidContract.Commands.Approve(), Arrays.asList(inputState.getBorrower().getOwningKey(),inputState.getLender().getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            FlowSession cpSession = initiateFlow(inputState.getLender());

            //step 6: collect signatures
            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(cpSession)));


            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(stx, Arrays.asList(cpSession)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void> {
        //private variable
        private FlowSession counterpartySession;

        //Constructor
        public Responder(FlowSession counterpartySession) {
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
