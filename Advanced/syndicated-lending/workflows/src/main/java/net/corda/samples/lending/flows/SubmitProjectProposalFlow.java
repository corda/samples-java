package net.corda.samples.lending.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.lending.contracts.ProjectContract;
import net.corda.samples.lending.states.ProjectState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SubmitProjectProposalFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{

        private Party borrower;
        private List<Party> lenders;
        private String projectDescription;
        private int projectCost;
        private int loanAmount;

        //public constructor
        public Initiator(List<Party> lenders, String projectDescription, int projectCost, int loanAmount) {
            this.lenders = lenders;
            this.projectDescription = projectDescription;
            this.projectCost = projectCost;
            this.loanAmount = loanAmount;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            this.borrower = getOurIdentity();

            // Step 1. Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));


            //Compose the State that carries the Hello World message
            final ProjectState output = new ProjectState(new UniqueIdentifier(), projectDescription, borrower,
                            projectCost, loanAmount, lenders);

            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the project as an output state, as well as a command to the transaction builder.
            builder.addOutputState(output);
            builder.addCommand(new ProjectContract.Commands.ProposeProject(), Collections.singletonList(borrower.getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> cpSesions = lenders.stream().map(this::initiateFlow).collect(Collectors.toList());

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(ptx, cpSesions));
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
