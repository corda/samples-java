package net.corda.samples.auction.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;

import java.util.Currency;
import java.util.UUID;

/**
 * This purpose of this flows is the settle the auction which includes:
 *  - Transferring the highest bid amount to the auctioneer.
 *  - Transferring the auctioned asset's ownership to the highest bidder
 *  - Consuming the auction states.
 *
 *  The AuctionDvPFlow does the delivery-vs-payment of the auctioned asset and the bid amount.
 *  The AuctionExitFlow consumes the auction states.
 */
@StartableByRPC
public class AuctionSettlementFlow extends FlowLogic<Void> {

    private final UUID auctionId;
    private final Amount<Currency> amount;

    /**
     * Constructor to initialise flows parameters received from rpc.
     *
     * @param auctionId is the unique id of the auction to be settled
     * @param amount is the bid amount which is required to be transferred from the highest bidded to auctioneer to
     *              settle the auction.
     */
    public AuctionSettlementFlow(UUID auctionId, Amount<Currency> amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new AuctionDvPFlow.Initiator(auctionId, amount));
        subFlow(new AuctionExitFlow.Initiator(auctionId));
        return null;
    }
}
