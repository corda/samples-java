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

            // Step 1. Get a reference to the notary service on our network and our key pair.
            // Note: ongoing work to support multiple notary identities is still in progress.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

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

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the inputs and outputs, as well as a command to the transaction builder.
            builder.addInputState(inputStateAndRef);
            builder.addOutputState(output);
            builder.addCommand(new LoanBidContract.Commands.Approve(), Arrays.asList(inputState.getBorrower().getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            FlowSession cpSession = initiateFlow(inputState.getLender());

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(ptx, Arrays.asList(cpSession)));
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

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }

}
