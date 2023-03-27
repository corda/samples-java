package net.corda.samples.auction.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.samples.auction.contracts.AuctionContract
import net.corda.samples.auction.states.AuctionState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class CreateAuctionFlow(private val basePrice: Amount<Currency>,
                        private val auctionItem: UUID,
                        private val bidDeadline: LocalDateTime) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():SignedTransaction {

        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val auctioneer = ourIdentity

        // Fetch all parties from the network map and remove the auctioneer and notary. All the parties are added as
        // participants to the auction state so that its visible to all the parties in the network.
        val bidders : List<Party> = serviceHub.networkMapCache.allNodes.map { it.legalIdentities.get(0) } - auctioneer

        // Create the output state. Use a linear pointer to point to the asset on auction. The asset would be added
        // as a reference state to the transaction and hence we won't spend it.
        val output = AuctionState(LinearPointer(UniqueIdentifier(null, auctionItem), LinearState::class.java),
                UUID.randomUUID(), basePrice, null, null, bidDeadline.atZone(ZoneId.systemDefault()).toInstant(),
                null, true, auctioneer, bidders, null)

        // Build the transaction
        val txbuilder = TransactionBuilder(notary)
                .addOutputState(output)
                .addCommand(AuctionContract.Commands.CreateAuction(), listOf(auctioneer.owningKey))

        // Verify the transaction
        txbuilder.verify(serviceHub)
        // Sign the transaction
        val stx = serviceHub.signInitialTransaction(txbuilder)
        val bidderSessions = bidders.map{initiateFlow(it)}
        return subFlow(FinalityFlow(stx,bidderSessions))
    }
}

@InitiatedBy(CreateAuctionFlow::class)
class CreateAuctionFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
