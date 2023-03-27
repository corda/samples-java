package net.corda.samples.heartbeat.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SchedulableFlow
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.samples.heartbeat.contracts.HeartContract.Commands.Beat
import net.corda.samples.heartbeat.contracts.HeartContract.Companion.contractID
import net.corda.samples.heartbeat.states.HeartState

/**
 * This is the flow that a Heartbeat state runs when it consumes itself to create a new Heartbeat
 * state on the ledger.
 *
 * @param stateRef the existing Heartbeat state to be updated.
 */
@InitiatingFlow
@SchedulableFlow
class HeartbeatFlow(private val stateRef: StateRef) : FlowLogic<String>() {
    companion object {
        object GENERATING_TRANSACTION : Step("Generating a HeartState transaction.")
        object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
        object FINALISING_TRANSACTION : Step("Recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): String {
        progressTracker.currentStep = GENERATING_TRANSACTION
        val input = serviceHub.toStateAndRef<HeartState>(stateRef)
        val output = HeartState(ourIdentity)
        val beatCmd = Command(Beat(), ourIdentity.owningKey)

        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")) // METHOD 2

        val txBuilder = TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(output, contractID)
                .addCommand(beatCmd)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = FINALISING_TRANSACTION
        subFlow(FinalityFlow(signedTx, listOf()))
        // The sound of a heart.
        return "Lub-dub"
    }
}
