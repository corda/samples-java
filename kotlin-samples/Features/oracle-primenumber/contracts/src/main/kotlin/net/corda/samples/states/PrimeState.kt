package net.corda.samples.states

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.samples.contracts.PrimeContract


// If 'n' is a natural number N then 'nthPrime' is the Nth prime.
// `Requester` is the Party that will store this fact in its vault.
@BelongsToContract(PrimeContract::class)
data class PrimeState(val n: Int,
                      val nthPrime: Int,
                      val requester: AbstractParty) : ContractState {
    override val participants: List<AbstractParty> get() = listOf(requester)
    override fun toString() = "The ${n}th prime number is $nthPrime."
}
