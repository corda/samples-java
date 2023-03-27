package net.corda.samples.statereissuance.flows.reissuance

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import com.r3.corda.lib.reissuance.flows.RequestReissuance
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.samples.statereissuance.contracts.LandTitleContract
import net.corda.samples.statereissuance.states.LandTitleState
import java.util.*

@InitiatingFlow
@StartableByRPC
class RequestReissueLandState(val issuer: Party,
                              val plotIdentifier: UniqueIdentifier) : FlowLogic<SecureHash>() {

    @Suspendable
    override fun call(): SecureHash {
        val landTitleStateAndRefs = serviceHub.vaultService.queryBy(LandTitleState::class.java).states

        val stateAndRef = landTitleStateAndRefs.stream().filter { it.state.data.linearId == plotIdentifier }
                .findAny().orElseThrow { IllegalArgumentException("Land Not Found") }

        return subFlow(RequestReissuance<LandTitleState>(issuer, Arrays.asList(stateAndRef.ref), LandTitleContract.Commands.Issue(),
                listOf(), null)
        )
    }
}

