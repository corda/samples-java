package net.corda.samples.sendfile.states

import net.corda.samples.sendfile.contracts.InvoiceContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

// *********
// * State *
// *********
@BelongsToContract(InvoiceContract::class)
data class InvoiceState(
        val invoiceAttachmentID: String,
        override val participants: List<AbstractParty> = listOf()) : ContractState
