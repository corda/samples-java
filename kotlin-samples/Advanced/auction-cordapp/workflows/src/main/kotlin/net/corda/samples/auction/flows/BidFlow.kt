package net.corda.samples.auction.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.samples.auction.contracts.AuctionContract
import net.corda.samples.auction.states.AuctionState
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class BidFlow(private val auctionId:UUID,
              private val bidAmount: Amount<Currency>) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():SignedTransaction {
        // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionId
        // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
        // transaction.
        val auctionStateAndRefs = serviceHub.vaultService.queryBy<AuctionState>().states
        val inputStateAndRef = auctionStateAndRefs.filter {
            val auctionState = it.state.data
            auctionState.auctionId == this.auctionId
        }[0]

        //get the notary from the input state.
        val notary = inputStateAndRef.state.notary
        val inputState = inputStateAndRef.state.data

        //Create the output state
        val output = inputState.copy(highestBid = bidAmount,highestBidder = ourIdentity)

        // Build the transaction. On successful completion of the transaction the current auction state is consumed
        // and a new auction state is create as an output containg tge bid details.
        val txBuilder = TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(output)
                .addCommand(AuctionContract.Commands.Bid(),listOf(ourIdentity.owningKey))

        // Verify the transaction
        txBuilder.verify(serviceHub)

        // Sign the transaction
        val stx = serviceHub.signInitialTransaction(txBuilder)

        val bidderSessions = inputState.bidders!!.minus(ourIdentity).plus(inputState.auctioneer).map { initiateFlow(it!!) }
        return subFlow(FinalityFlow(stx, bidderSessions))
    }
}

@InitiatedBy(BidFlow::class)
class BidFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
