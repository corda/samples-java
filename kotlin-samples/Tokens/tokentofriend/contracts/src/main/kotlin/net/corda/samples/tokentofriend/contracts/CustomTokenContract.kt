package net.corda.samples.tokentofriend.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.samples.tokentofriend.states.CustomTokenState
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class CustomTokenContract: EvolvableTokenContract(), Contract{

    companion object {
        @JvmStatic
        val CONTRACT_ID = "net.corda.samples.tokentofriend.contracts.CustomTokenContract"
    }

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        val newToken = tx.outputStates.single() as CustomTokenState
        newToken.apply {
            require(recipient != "") {"Recipient Email cannot be empty"}
            require(recipient != issuer) {"Token Issuer Email and token recipient Email cannot be the same"}
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        /*This additional check does not apply to this use case.
         *This sample does not allow token update */
    }
}