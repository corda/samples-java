package net.corda.samples.statereissuance.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.statereissuance.contracts.LandTitleContract;
import net.corda.samples.statereissuance.states.LandTitleState;

import java.util.Arrays;
import java.util.List;

public class TransferLandTitleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier plotIdentifier;
        private Party owner;

        public Initiator(UniqueIdentifier plotIdentifier, Party owner) {
            this.plotIdentifier = plotIdentifier;
            this.owner = owner;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            List<StateAndRef<LandTitleState>> landTitleStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(LandTitleState.class).getStates();

            StateAndRef<LandTitleState> inputStateAndRef = landTitleStateAndRefs.stream().filter(landTitleStateStateAndRef -> {
                LandTitleState loanBidState = landTitleStateStateAndRef.getState().getData();
                return loanBidState.getLinearId().equals(plotIdentifier);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Land Title Not Found"));

            LandTitleState inputState = inputStateAndRef.getState().getData();
            LandTitleState outputState = new LandTitleState(inputState.getLinearId(), inputState.getDimensions(),
                    inputState.getArea(), owner, inputState.getIssuer());

            TransactionBuilder transactionBuilder = new TransactionBuilder(inputStateAndRef.getState().getNotary())
                    .addInputState(inputStateAndRef)
                    .addOutputState(outputState)
                    .addCommand(new LandTitleContract.Commands.Transfer(), Arrays.asList(
                            inputState.getIssuer().getOwningKey(),
                            inputState.getOwner().getOwningKey()
                    ));

            transactionBuilder.verify(getServiceHub());
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(transactionBuilder);

            FlowSession issuerSession = initiateFlow(inputState.getIssuer());
            issuerSession.send(true);
            FlowSession newOwnerSession = initiateFlow(owner);
            newOwnerSession.send(false);

            SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, Arrays.asList(issuerSession)));
            return subFlow(new FinalityFlow(stx, Arrays.asList(issuerSession, newOwnerSession)));
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

            boolean signRequired = counterpartySession.receive(Boolean.class).unwrap(it -> it);
            if(signRequired) {
                subFlow(new SignTransactionFlow(counterpartySession) {
                    @Suspendable
                    @Override
                    protected void checkTransaction(SignedTransaction stx) throws FlowException {

                    }
                });
            }

            //Stored the transaction into data base.
            subFlow(new ReceiveFinalityFlow(counterpartySession));
            return null;
        }
    }
}
