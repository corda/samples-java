package net.corda.samples.autopayroll.states

import net.corda.samples.autopayroll.contracts.PaymentRequestContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
@BelongsToContract(PaymentRequestContract::class)
data class PaymentRequestState(
        val amount: String,
        val toWhom: Party,
        override val participants: List<AbstractParty> = listOf()) : ContractState
