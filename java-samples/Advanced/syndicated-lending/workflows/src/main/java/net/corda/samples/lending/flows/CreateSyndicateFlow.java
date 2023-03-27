package net.corda.samples.lending.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StaticPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.lending.contracts.SyndicateContract;
import net.corda.samples.lending.states.LoanBidState;
import net.corda.samples.lending.states.ProjectState;
import net.corda.samples.lending.states.SyndicateState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CreateSyndicateFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private Party leadBank;
        private List<Party> participantBanks;
        private UniqueIdentifier projectIdentifier;
        private UniqueIdentifier loanDetailIdentifier;

        //public constructor
        public Initiator(List<Party> participantBanks, UniqueIdentifier projectIdentifier, UniqueIdentifier loanDetailIdentifier) {
            this.participantBanks = participantBanks;
            this.projectIdentifier = projectIdentifier;
            this.loanDetailIdentifier = loanDetailIdentifier;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Step 1. Get a reference to the notary service on our network and our key pair.
            /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));


            this.leadBank = getOurIdentity();

            // Fetch Project
            List<StateAndRef<ProjectState>> projectStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(ProjectState.class).getStates();

            StateAndRef<ProjectState> projectDetailStateAndRef = projectStateAndRefs.stream().filter(projectStateAndRef -> {
                ProjectState projectState = projectStateAndRef.getState().getData();
                return projectState.getLinearId().equals(projectIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Project Not Found"));

            // Fetch Loan
            List<StateAndRef<LoanBidState>> loanBidStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(LoanBidState.class).getStates();

            StateAndRef<LoanBidState> loanStateAndRef = loanBidStateAndRefs.stream().filter(loanBidStateAndRef -> {
                LoanBidState loanBidState = loanBidStateAndRef.getState().getData();
                return loanBidState.getLinearId().equals(loanDetailIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Loan Details Not Found"));

            SyndicateState syndicateState = new SyndicateState(
                    new UniqueIdentifier(), leadBank, participantBanks,
                    new LinearPointer<>(projectIdentifier, ProjectState.class),
                    new LinearPointer<>(loanDetailIdentifier, LoanBidState.class)
                    //Collections.emptyList()
            );


            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the project as an output state, as well as a command to the transaction builder.
            builder.addOutputState(syndicateState);
            builder.addCommand(new SyndicateContract.Commands.Create(), Arrays.asList(syndicateState.getLeadBank().getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            List<FlowSession> cpSesions = participantBanks.stream().map(this::initiateFlow).collect(Collectors.toList());

            // Step 7. Assuming no exceptions, we can now finalise the transaction
            return subFlow(new FinalityFlow(ptx, cpSesions));
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
            subFlow(new ReceiveFinalityFlow(counterpartySession, null, StatesToRecord.ALL_VISIBLE));
            return null;
        }
    }

}
