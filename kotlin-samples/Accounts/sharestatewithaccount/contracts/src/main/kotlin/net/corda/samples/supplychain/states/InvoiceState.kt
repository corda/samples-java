package net.corda.samples.supplychain.states

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.samples.supplychain.contracts.InvoiceStateContract
import java.util.*


@BelongsToContract(InvoiceStateContract::class)
class InvoiceState(

        val amount: Int,
        val sender: AnonymousParty,
        val recipient: AnonymousParty,
        val invoiceID: UUID) : ContractState{
    override val participants: List<AbstractParty> get() = listOfNotNull(recipient,sender).map { it }
}