package net.corda.samples.auction.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.samples.auction.contracts.AuctionContract
import net.corda.samples.auction.states.AuctionState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.util.*
import kotlin.math.sign

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class AuctionExitFlow(private val auctionId:UUID) : FlowLogic<SignedTransaction>() {
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
        val auctionState = inputStateAndRef.state.data

        // Decide who should be the signers of the transaction based on whether the auction has received bids. The
        // highest bidder must sign to avoid consuming a auction that's not settled yet.
        val signers = listOf(auctionState.auctioneer!!.owningKey)
        if (auctionState.winner != null){
            signers.plus(auctionState.winner!!.owningKey)
        }

        // Build the transaction to consume to the transaction.
        val txBuilder = TransactionBuilder(inputStateAndRef.state.notary)
                .addInputState(inputStateAndRef)
                .addCommand(AuctionContract.Commands.Exit(), signers)

        // Verify the transaction
        txBuilder.verify(serviceHub)

        // Sign the transaction
        var stx = serviceHub.signInitialTransaction(txBuilder) //I am calling from PartyB
        if (auctionState.winner != null){
            if(auctionState.auctioneer == ourIdentity){
                val winnerSession = initiateFlow(auctionState.winner!!)
                winnerSession.send(true)
                stx = subFlow(CollectSignaturesFlow(stx, listOf(winnerSession)))
            }else{
                val auctioneerSession = initiateFlow(auctionState.auctioneer!!)
                auctioneerSession.send(true)
                stx = subFlow(CollectSignaturesFlow(stx, listOf(auctioneerSession)))
            }
        }
        val allSession: MutableList<FlowSession> = mutableListOf<FlowSession>().toMutableList()
        for (party in auctionState.participants.filter { it != ourIdentity }){
            val session = initiateFlow(party)
            session.send(false)
            allSession += session
        }
        return subFlow(FinalityFlow(stx,allSession))
    }
}

@InitiatedBy(AuctionExitFlow::class)
class AuctionExitFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        val flag = counterpartySession.receive<Boolean>().unwrap{it -> it}
        if(flag){
            subFlow(object : SignTransactionFlow(counterpartySession) {
                override fun checkTransaction(stx: SignedTransaction){
                }
            })
        }
        return subFlow(ReceiveFinalityFlow(counterpartySession))
        }
}



