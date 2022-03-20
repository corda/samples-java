package net.corda.samples.statereissuance.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.statereissuance.contracts.LandTitleContract;
import net.corda.samples.statereissuance.states.LandTitleState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExitLandTitleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private StateRef stateRef;

        public Initiator(StateRef stateRef) {
            this.stateRef = stateRef;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            List<StateAndRef<LandTitleState>> landTitleStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(LandTitleState.class).getStates();

            StateAndRef<LandTitleState> inputStateAndRef = landTitleStateAndRefs.stream()
                    .filter(landTitleStateStateAndRef -> landTitleStateStateAndRef.getRef().equals(stateRef))
                    .findAny().orElseThrow(() -> new IllegalArgumentException("Land Title Not Found"));

            LandTitleState input = inputStateAndRef.getState().getData();

            TransactionBuilder builder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addCommand(new LandTitleContract.Commands.Exit(), Arrays.asList(getOurIdentity().getOwningKey(),
                            input.getIssuer().getOwningKey()));

            builder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

            FlowSession issuerSession = initiateFlow(input.getIssuer());

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Collections.singletonList(issuerSession)));
            return subFlow(new FinalityFlow(stx, Collections.singletonList(issuerSession)));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<Void>{

        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {

            subFlow(new SignTransactionFlow(counterpartySession) {
                @Suspendable
                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {

                }
            });

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }
}
