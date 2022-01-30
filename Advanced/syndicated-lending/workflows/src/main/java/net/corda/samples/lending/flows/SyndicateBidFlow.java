package net.corda.samples.lending.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.lending.contracts.SyndicateBidContract;
import net.corda.samples.lending.states.SyndicateBidState;
import net.corda.samples.lending.states.SyndicateState;

import java.util.Arrays;
import java.util.List;

public class SyndicateBidFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
        private UniqueIdentifier syndicateIdentifier;
        private int bidAmount;

        //public constructor
        public Initiator(UniqueIdentifier syndicateIdentifier, int bidAmount) {
            this.syndicateIdentifier = syndicateIdentifier;
            this.bidAmount = bidAmount;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Step 1. Get a reference to the notary service on our network and our key pair.
            // Note: ongoing work to support multiple notary identities is still in progress.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Fetch Project
            List<StateAndRef<SyndicateState>> syndicateStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(SyndicateState.class).getStates();

            StateAndRef<SyndicateState> syndicateStateAndRef = syndicateStateAndRefs.stream().filter(synStateAndRef -> {
                SyndicateState syndicateState = synStateAndRef.getState().getData();
                return syndicateState.getLinearId().equals(syndicateIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Syndicate Not Found"));

            SyndicateState syndicateState = syndicateStateAndRef.getState().getData();

            SyndicateBidState syndicateBidState = new SyndicateBidState(
                    new UniqueIdentifier(),
                    new LinearPointer<>(syndicateIdentifier, SyndicateState.class),
                    bidAmount,
                    syndicateState.getLeadBank(),
                    getOurIdentity(),
                    "SUBMITTED"
            );


            // Step 3. Create a new TransactionBuilder object.
            final TransactionBuilder builder = new TransactionBuilder(notary);

            // Step 4. Add the project as an output state, as well as a command to the transaction builder.
            builder.addOutputState(syndicateBidState);
            builder.addCommand(new SyndicateBidContract.Commands.Submit(), Arrays.asList(getOurIdentity().getOwningKey()));

            // Step 5. Verify and sign it with our KeyPair.
            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            FlowSession cpSession = initiateFlow(syndicateState.getLeadBank());

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
