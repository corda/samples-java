package net.corda.samples.auction.states

import net.corda.samples.auction.contracts.AuctionContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.time.Instant
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(AuctionContract::class)
data class AuctionState(
        val auctionItem: LinearPointer<LinearState>?,
        val auctionId: UUID?,
        val basePrice: Amount<Currency>,
        val highestBid: Amount<Currency>?,
        val highestBidder: Party?,
        val bidEndTime: Instant?,
        val winningBid: Amount<Currency>?,
        val active: Boolean,
        val auctioneer: Party?,
        val bidders: List<Party>?,
        val winner: Party?,
        override val participants: List<AbstractParty> = (bidders!! + auctioneer!!)) : SchedulableState {

    override fun nextScheduledActivity(thisStateRef: StateRef, flowLogicRefFactory: FlowLogicRefFactory): ScheduledActivity? {
        if(!active){
            return null
        }
        val flowLogicRef = flowLogicRefFactory.create("net.corda.samples.auction.flows.EndAuctionFlow",auctionId)
        return ScheduledActivity(flowLogicRef,bidEndTime!!)
    }


}
