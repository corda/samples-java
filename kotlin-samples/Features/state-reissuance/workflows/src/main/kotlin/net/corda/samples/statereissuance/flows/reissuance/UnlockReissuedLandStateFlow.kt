package net.corda.samples.statereissuance.flows.reissuance

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStates
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStatesResponder
import com.r3.corda.lib.reissuance.states.ReissuanceLock
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.samples.statereissuance.contracts.LandTitleContract
import net.corda.samples.statereissuance.states.LandTitleState

@StartableByRPC
class UnlockReissuedLandStateFlow(
        private val reissuedRef: StateRef,
        private val reissuanceLockRef: StateRef,
        private val exitTrnxId: SecureHash
): FlowLogic<SecureHash>() {

    @Suspendable
    override fun call(): SecureHash {

        val reissuanceLockStateAndRef = serviceHub.vaultService.queryBy<ReissuanceLock<LandTitleState>>(
                criteria= QueryCriteria.VaultQueryCriteria(stateRefs = listOf(reissuanceLockRef))
        ).states[0]

        val reissuedStatesStateAndRefs = serviceHub.vaultService.queryBy<LandTitleState>(
                criteria= QueryCriteria.VaultQueryCriteria(stateRefs = listOf(reissuedRef))
        ).states

        return subFlow(UnlockReissuedStates(
                reissuedStatesStateAndRefs,
                reissuanceLockStateAndRef,
                listOf(exitTrnxId),
                LandTitleContract.Commands.Reissue()
        ))
    }
}

@InitiatedBy(UnlockReissuedStates::class)
class UnlockReissuedLandStateResponder(
        otherSession: FlowSession
) : UnlockReissuedStatesResponder(otherSession) {

    override fun checkConstraints(stx: SignedTransaction) {

    }
}