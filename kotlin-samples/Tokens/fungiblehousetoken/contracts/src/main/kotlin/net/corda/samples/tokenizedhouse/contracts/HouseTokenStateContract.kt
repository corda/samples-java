package net.corda.samples.tokenizedhouse.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState

// ************
// * Contract *
// ************
class HouseTokenStateContract : EvolvableTokenContract(), Contract {
    companion object {
        const val CONTRACT_ID = "net.corda.samples.tokenizedhouse.contracts.HouseTokenStateContract"
    }
    override fun additionalCreateChecks(tx: LedgerTransaction) {
        // Write contract validation logic to be performed while creation of token
        val outputState = tx.getOutput(0) as FungibleHouseTokenState
        outputState.apply {
            require(outputState.valuation > 0) {"Valuation must be greater than zero"}
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        // Write contract validation logic to be performed while updation of token
    }

}