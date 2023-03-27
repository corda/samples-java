package net.corda.samples.supplychain.states

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.samples.supplychain.contracts.InternalMessageStateContract


@BelongsToContract(InternalMessageStateContract::class)
class InternalMessageState(

        val task: String,
        val from: AnonymousParty,
        val to: AnonymousParty) : ContractState{
    override val participants: List<AbstractParty> get() = listOfNotNull(from,to).map { it }
}