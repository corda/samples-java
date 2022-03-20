package net.corda.samples.statereissuance.flows.reissuance.kotlin

import net.corda.core.flows.InitiatedBy
import com.r3.corda.lib.reissuance.flows.ReissueStates
import net.corda.core.flows.FlowSession
import com.r3.corda.lib.reissuance.flows.ReissueStatesResponder
import net.corda.core.transactions.SignedTransaction

// Added Kotlin code because of a bug which doesnot allow to write Java

@InitiatedBy(ReissueStates::class)
class AcceptLandReissuanceResponder(
        otherSession: FlowSession
) : ReissueStatesResponder(otherSession) {

    override fun checkConstraints(stx: SignedTransaction) {

    }
}