package net.corda.samples.supplychain.states

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty

import net.corda.core.identity.AnonymousParty
import net.corda.samples.supplychain.contracts.PaymentStateContract


@BelongsToContract(PaymentStateContract::class)
class PaymentState(

        val amount: Int,
        val recipient: AnonymousParty,
        val sender: AnonymousParty) : ContractState{
    override val participants: List<AbstractParty> get() = listOfNotNull(recipient,sender).map { it }
}