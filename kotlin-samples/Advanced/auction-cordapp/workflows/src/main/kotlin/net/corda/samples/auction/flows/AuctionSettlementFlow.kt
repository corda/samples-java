package net.corda.samples.auction.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.utilities.ProgressTracker
import java.util.*

// *********
// * Flows *
// *********
@StartableByRPC
class AuctionSettlementFlow(private val auctionId:UUID,
                            private val amount: Amount<Currency>) : FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        subFlow(AuctionDvPFlow(auctionId, amount))
        subFlow(AuctionExitFlow(auctionId))
    }
}
