package net.corda.samples.notarychange.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.flows.AbstractStateReplacementFlow.Instigator.Companion.tracker
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.notarychange.states.IOUState

@InitiatingFlow
@StartableByRPC
class SwitchNotaryFlow(private val linearId: UniqueIdentifier, private val newNotary: Party) : FlowLogic<String>() {
    private val QUERYING_VAULT = ProgressTracker.Step("Fetching IOU from node's vault.")
    private val INITITATING_TRANSACTION: ProgressTracker.Step = object : ProgressTracker.Step("Initiating Notary Change Transaction") {
        override fun childProgressTracker(): ProgressTracker? {
            return tracker()
        }
    }
    override val progressTracker = ProgressTracker(
            QUERYING_VAULT,
            INITITATING_TRANSACTION
    )

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {
        progressTracker.currentStep = QUERYING_VAULT
        val queryCriteria: QueryCriteria = LinearStateQueryCriteria(null, listOf(linearId.id))
        val (states) = serviceHub.vaultService.queryBy(IOUState::class.java, queryCriteria)
        if (states.size == 0) {
            throw FlowException("No IOU found for LinearId:$linearId")
        }
        progressTracker.currentStep = INITITATING_TRANSACTION
        subFlow(NotaryChangeFlow(states[0], newNotary, tracker()))
        return "Notary Switched Successfully"
    }
}