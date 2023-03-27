package net.corda.samples.supplychain.states

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.samples.supplychain.contracts.ShippingRequestStateContract


@BelongsToContract(ShippingRequestStateContract::class)
class ShippingRequestState(

        val pickUpFrom: AnonymousParty,
        val DeliverTo: String,
        val shippper: Party,
        val cargo: String) : ContractState{
    override val participants: List<AbstractParty> get() = listOfNotNull(pickUpFrom).map { it }
}