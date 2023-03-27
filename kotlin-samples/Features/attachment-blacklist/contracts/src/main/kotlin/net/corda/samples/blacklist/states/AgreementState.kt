package net.corda.samples.blacklist.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party
import net.corda.samples.blacklist.contracts.AgreementContract

@BelongsToContract(AgreementContract::class)
data class AgreementState(val partyA: Party, val partyB: Party, val txt: String) : ContractState {
    override val participants get() = listOf(partyA, partyB)
}
