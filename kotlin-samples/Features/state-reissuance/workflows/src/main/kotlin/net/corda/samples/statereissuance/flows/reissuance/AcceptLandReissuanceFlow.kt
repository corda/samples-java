package net.corda.samples.statereissuance.flows.reissuance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.flows.ReissueStates
import com.r3.corda.lib.reissuance.flows.ReissueStatesResponder
import com.r3.corda.lib.reissuance.states.ReissuanceRequest
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.util.*

@InitiatingFlow
@StartableByRPC
class AcceptLandReissuance(private val issuer: Party,
                           private val stateRef: StateRef) : FlowLogic<SecureHash>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SecureHash {

        val reissueRequestStateAndRefs = serviceHub.vaultService.queryBy(ReissuanceRequest::class.java).states

        val stateAndRef = reissueRequestStateAndRefs.stream().filter { it.state.data.stateRefsToReissue.contains(stateRef) }
                .findAny().orElseThrow { IllegalArgumentException("ReIssuance Request does not exist") }

        return subFlow(ReissueStates<ContractState>(stateAndRef, Arrays.asList(issuer)))
    }
}

@InitiatedBy(ReissueStates::class)
class AcceptLandReissuanceResponder(otherSession: FlowSession) : ReissueStatesResponder(otherSession) {

    override fun checkConstraints(stx: SignedTransaction) {

    }
}