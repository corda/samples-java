package net.corda.samples.bikemarket.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.bikemarket.states.FrameTokenState
import net.corda.samples.bikemarket.states.WheelsTokenState

// ************
// * Contract *
// ************
class WheelsContract: EvolvableTokenContract(), Contract{
    override fun additionalCreateChecks(tx: LedgerTransaction) {
        val newToken = tx.outputStates.single() as WheelsTokenState
        newToken.apply {
            require(serialNum != "") {"serialNum cannot be empty"}
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        /*This additional check does not apply to this use case.
         *This sample does not allow token update */
    }

    companion object {
        const val CONTRACT_ID = "net.corda.samples.bikemarket.contracts.WheelsContract"
    }
}