package net.corda.samples.auction.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.auction.contracts.AuctionContract;
import net.corda.samples.auction.states.AuctionState;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * This flows is used to exit an auction states once it has been settled or auction did not received bids till the
 * deadline.
 *
 * The flows is initiated by the highest bidder in case as part of auction settlement flows.
 * In case of no bids received, it can be initiated by the auctioneer to exit the auction.
 */
public class AuctionExitFlow {

    @StartableByRPC
    @InitiatingFlow
    public static class AuctionExitInitiator extends FlowLogic<SignedTransaction>{
        private UUID auctionId;

        /**
         *
         * @param auctionId is the unique id of the auction to be consumed.
         */
        public AuctionExitInitiator(UUID auctionId) {
            this.auctionId = auctionId;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {

            // Query the vault to fetch a list of all AuctionState states, and filter the results based on the auctionId
            // to fetch the desired AuctionState states from the vault. This filtered states would be used as input to the
            // transaction.
            List<StateAndRef<AuctionState>> auntionStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionState.class).getStates();
            StateAndRef<AuctionState> auctionStateAndRef = auntionStateAndRefs.stream().filter(stateAndRef -> {
                AuctionState auctionState = stateAndRef.getState().getData();
                return auctionState.getAuctionId().equals(auctionId);
            }).findAny().orElseThrow(() -> new FlowException("Auction Not Found"));
            AuctionState auctionState = auctionStateAndRef.getState().getData();

            // Decide who should be the signers of the transaction based on whether the auction has received bids. The
            // highest bidder must sign to avoid consuming a auction that's not settled yet.
            List<PublicKey> signers = new ArrayList<>();
            signers.add(auctionState.getAuctioneer().getOwningKey());
            if(auctionState.getWinner()!=null){
                signers.add(auctionState.getWinner().getOwningKey());
            }

            // Build the transaction to consume to the transaction.
            TransactionBuilder transactionBuilder = new TransactionBuilder(auctionStateAndRef.getState().getNotary())
                    .addInputState(auctionStateAndRef)
                    .addCommand(new AuctionContract.Commands.Exit(), signers);

            // Verify the transaction
            transactionBuilder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            List<FlowSession> allSessions = new ArrayList<>();

            // Collect Signature from appropriate counterparty, depending on who initiated the transaction
            // i.e. auctioneer/ highest bidder
            if(auctionState.getWinner()!=null) {
                if(auctionState.getAuctioneer() == getOurIdentity()){
                    FlowSession winnerSession = initiateFlow(auctionState.getWinner());
                    winnerSession.send(true);
                    allSessions.add(winnerSession);
                    signedTransaction = subFlow(new CollectSignaturesFlow(
                            signedTransaction, Arrays.asList(winnerSession)));
                }else {
                    FlowSession auctioneerSession = initiateFlow(auctionState.getAuctioneer());
                    auctioneerSession.send(true);
                    allSessions.add(auctioneerSession);
                    signedTransaction = subFlow(new CollectSignaturesFlow(
                            signedTransaction, Arrays.asList(auctioneerSession)));
                }
            }

            // Initiate session will all participants, notarize and record update in all the participants ledger.
            for(Party party: auctionState.getBidders()){
                if(!party.equals(getOurIdentity())) {
                    FlowSession session = initiateFlow(party);
                    session.send(false);
                    allSessions.add(session);
                }
            }
            return subFlow(new FinalityFlow(signedTransaction, (allSessions)));
        }
    }

    @InitiatedBy(AuctionExitInitiator.class)
    public static class AuctionExitResponder extends FlowLogic<SignedTransaction> {
        private FlowSession otherPartySession;

        public AuctionExitResponder(FlowSession otherPartySession) {
            this.otherPartySession = otherPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            boolean flag = otherPartySession.receive(Boolean.class).unwrap(it -> it);
            // Flag to decide when CollectSignaturesFlow is called for this counterparty. SignTransactionFlow is
            // executed only if CollectSignaturesFlow is called from the initiator.
            if(flag) {
                subFlow(new SignTransactionFlow(otherPartySession) {

                    @Override
                    protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                    }
                });
            }
            return subFlow(new ReceiveFinalityFlow(otherPartySession));
        }
    }
}
