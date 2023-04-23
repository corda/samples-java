package net.corda.samples.auction.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.samples.auction.contracts.AuctionContract;
import net.corda.samples.auction.contracts.AuctionExpiryContract;
import net.corda.samples.auction.states.AuctionExpiry;
import net.corda.samples.auction.states.AuctionState;

import java.util.*;

/**
 * This flows is used to put a bid on an asset put on auction.
 */
public class BidFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class BidInitiator extends FlowLogic<SignedTransaction>{

        private final Amount<Currency> bidAmount;
        private final UUID auctionId;

        /**
         * Constructor to initialise flows parameters received from rpc.
         *
         * @param bidAmount is the amount the bidder is bidding for for the asset on auction.
         * @param auctionId is the unique identifier of the auction on which this bid it put.
         */
        public BidInitiator(Amount<Currency> bidAmount, UUID auctionId) {
            this.bidAmount = bidAmount;
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
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Auction Not Found"));

            List<StateAndRef<AuctionExpiry>> expiryStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(AuctionExpiry.class).getStates();

            StateAndRef<AuctionExpiry> expiryStateAndRef = expiryStateAndRefs.stream().filter(stateAndRef -> {
                AuctionExpiry expiryState = stateAndRef.getState().getData();
                return expiryState.getAuctionId().equals(auctionId);
            }).findAny().orElseThrow(() -> new IllegalArgumentException("Expiry Not Found"));

            AuctionState inputAuction = auctionStateAndRef.getState().getData();
            AuctionExpiry inputExpiry = expiryStateAndRef.getState().getData();
            Party notary = auctionStateAndRef.getState().getNotary();

            //Create the output states
            AuctionState outputAuction = new AuctionState(inputAuction.getAuctionItem(),
                    inputAuction.getAuctionId(), inputAuction.getBasePrice(),
                    bidAmount, getOurIdentity(),
                    inputAuction.getBidEndTime(),
                    null, true,
                    inputAuction.getAuctioneer(), inputAuction.getBidders(), null);

            AuctionExpiry outputExpiry = new AuctionExpiry(inputExpiry.getExpiry(),
                    inputExpiry.getAuctionId(), new ArrayList(inputExpiry.getParticipants()));

            // Build the transaction. On successful completion of the transaction the current auction states is consumed
            // and a new auction states is create as an output containg tge bid details.
            TransactionBuilder builder = new TransactionBuilder(notary)
                    .addInputState(auctionStateAndRef)
                    .addInputState(expiryStateAndRef)
                    .addOutputState(outputAuction, AuctionContract.ID, notary, 1)
                    .addOutputState(outputExpiry, AuctionExpiryContract.ID, notary, 0)
                    .addCommand(new AuctionContract.Commands.Bid(),
                            Collections.singletonList(getOurIdentity().getOwningKey()));

            // Verify the transaction
            builder.verify(getServiceHub());

            // Sign the transaction
            SignedTransaction selfSignedTransaction = getServiceHub().signInitialTransaction(builder);

            // Call finality Flow to notarise and commit the transaction in all the participants ledger.
            List<FlowSession> allSessions = new ArrayList<>();
            List<Party> bidders = new ArrayList<>(inputAuction.getBidders());
            bidders.remove(getOurIdentity());
            for(Party bidder: bidders)
                allSessions.add(initiateFlow(bidder));

            allSessions.add(initiateFlow(inputAuction.getAuctioneer()));
            return subFlow(new FinalityFlow(selfSignedTransaction, allSessions));
        }
    }

    @InitiatedBy(BidInitiator.class)
    public static class BidResponder extends FlowLogic<SignedTransaction> {

        private FlowSession counterpartySession;

        public BidResponder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveFinalityFlow(counterpartySession));
        }
    }
}
