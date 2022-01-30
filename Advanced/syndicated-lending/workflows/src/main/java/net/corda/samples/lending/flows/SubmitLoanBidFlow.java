package net.corda.samples.lending.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StatePointer;
import net.corda.core.contracts.StaticPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.lending.contracts.LoanBidContract;
import net.corda.samples.lending.contracts.ProjectContract;
import net.corda.samples.lending.states.LoanBidState;
import net.corda.samples.lending.states.ProjectState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SubmitLoanBidFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{

        private Party borrower;
        private Party lender;
        private int loanAmount;
        private int tenure;
        private double rateofInterest;
        private int transactionFees;
        private UniqueIdentifier projectIdentifier;

        //public constructor
        public Initiator(Party borrower, int loanAmount, int tenure, double rateofInterest, int transactionFees,
                         UniqueIdentifier projectIdentifier) {
            this.borrower = borrower;
            this.loanAmount = loanAmount;
            this.tenure = tenure;
            this.rateofInterest = rateofInterest;
            this.transactionFees = transactionFees;
            this.projectIdentifier = projectIdentifier;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            this.lender = getOurIdentity();

            // Step 1. Get a reference to the notary service on our network and our key pair.
            // Note: ongoing work to support multiple notary identities is still in progress.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            List<StateAndRef<ProjectState>> projectStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(ProjectState.class).getStates();

            StateAndRef<ProjectState> inputStateAndRef = projectStateAndRefs.stream().filter(projectStateAndRef -> {
                ProjectState projectState = projectStateAndRef.getState().getData();
                return projectState.getLinearId().equals(projectIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Project Not Found"));


            final LoanBidState output = new LoanBidState(
                    new StaticPointer<>(inputStateAndRef.getRef(), ProjectState.class),
                    new UniqueIdentifier(),
                    lender, borrower,
                    loanAmount, tenure, rateofInterest, transactionFees,
                    "SUBMITTED"
            );

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the project as an output state, as well as a command to the transaction builder.
            builder.addOutputState(output);
            builder.addCommand(new LoanBidContract.Commands.Submit(), Arrays.asList(lender.getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            FlowSession cpSession = initiateFlow(borrower);

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(ptx, Arrays.asList(cpSession)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void>{
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
