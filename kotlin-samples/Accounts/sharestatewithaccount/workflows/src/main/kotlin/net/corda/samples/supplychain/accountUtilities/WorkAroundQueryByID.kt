package net.corda.samples.supplychain.accountUtilities

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.internal.accountObservedQueryBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.samples.supplychain.states.InvoiceState

@StartableByRPC
@StartableByService
@InitiatingFlow
class WorkAroundQueryByID(
    val acctname : String
) : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {
        val myAccount = accountService.accountInfo(acctname).single().state.data
        val invoices = serviceHub.vaultService.accountObservedQueryBy(listOf(myAccount.identifier.id),InvoiceState::class.java)
            .states.map { "\n" +"Invoice State : " +it.state.data.amount}

        return invoices
    }

}