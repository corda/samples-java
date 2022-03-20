package net.corda.samples.statereissuance.flows.reissuance.kotlin


import net.corda.core.flows.InitiatedBy
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStates
import com.r3.corda.lib.reissuance.flows.UnlockReissuedStatesResponder
import net.corda.core.flows.FlowSession
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(UnlockReissuedStates::class)
class UnlockReissuedLandStateResponder(
        otherSession: FlowSession
) : UnlockReissuedStatesResponder(otherSession) {

    override fun checkConstraints(stx: SignedTransaction) {

    }
}