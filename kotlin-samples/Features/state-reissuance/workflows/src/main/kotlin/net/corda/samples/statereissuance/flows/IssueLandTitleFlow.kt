package net.corda.samples.statereissuance.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.statereissuance.contracts.LandTitleContract
import net.corda.samples.statereissuance.states.LandTitleState


class IssueLandTitleFlow {
}

@InitiatingFlow
@StartableByRPC
class IssueLandTitle(private val owner: Party,
                     val dimension: String,
                     val area: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // Step 1. Get a reference to the notary service on our network and our key pair.
        // Note: ongoing work to support multiple notary identities is still in progress.
        val notary = serviceHub.networkMapCache.getNotary( CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val issuer = ourIdentity;

        val landTitleState = LandTitleState(UniqueIdentifier(),dimension,area,issuer,owner)

        val builder = TransactionBuilder(notary)
                .addOutputState(landTitleState)
                .addCommand(LandTitleContract.Commands.Issue(), listOf(issuer.owningKey))

        builder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(builder)

        // Step 7. Assuming no exceptions, we can now finalise the transaction
        return subFlow<SignedTransaction>(FinalityFlow(ptx, listOf(initiateFlow(owner))))
    }
}

@InitiatedBy(IssueLandTitle::class)
class IssueLandTitleResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}

