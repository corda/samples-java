package net.corda.samples.auction.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.samples.auction.contracts.AuctionContract
import net.corda.samples.auction.states.AuctionState
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
@SchedulableFlow
class EndAuctionFlow(private val auctionId: UUID) : FlowLogic<SignedTransaction?>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction? {
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

        // Check used to restrict the flow execution to be only done by the auctioneer.
        if(ourIdentity.owningKey == inputState.auctioneer!!.owningKey){
            val ouput = inputState.copy(active = false,
                    highestBidder = inputState.highestBidder,
                    winner = inputState.highestBidder,
                    winningBid = inputState.highestBid
                    )

            // Build the transaction. On successful completion of the transaction the current auction state is consumed
            // and a new auction state is create as an output containg tge bid details.
            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(ouput)
                    .addCommand(AuctionContract.Commands.EndAuction(),ourIdentity.owningKey)

            // Verify the transaction
            txBuilder.verify(serviceHub)
            // Sign the transaction
            val stx = serviceHub.signInitialTransaction(txBuilder)
            // Call finality Flow to notarise and commit the transaction in all the participants ledger.
            val bidderSessions = inputState.bidders!!.map { initiateFlow(it) }
            return subFlow(FinalityFlow(stx,bidderSessions))
        }
        return null
    }
}

@InitiatedBy(EndAuctionFlow::class)
class EndAuctionFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
