package net.corda.samples.statereissuance.flows.reissuance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.flows.ReissueStates
import com.r3.corda.lib.reissuance.flows.RejectReissuanceRequest
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import java.util.*

@InitiatingFlow
@StartableByRPC
class RejectLandReissuance(private val stateRef: StateRef,
                           private val issuer: Party) : FlowLogic<SecureHash>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SecureHash {

        val reissueRequestStateAndRefs = serviceHub.vaultService.queryBy(ReissuanceRequest::class.java).states

        val stateAndRef = reissueRequestStateAndRefs.stream().filter { it.state.data.stateRefsToReissue.contains(stateRef) }
                .findAny().orElseThrow { IllegalArgumentException("ReIssuance Request does not exist") }

        return subFlow(RejectReissuanceRequest<ContractState>(stateAndRef))
    }
}