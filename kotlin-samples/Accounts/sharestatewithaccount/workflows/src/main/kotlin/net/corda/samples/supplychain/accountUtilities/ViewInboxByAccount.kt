package net.corda.samples.supplychain.accountUtilities


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.samples.supplychain.states.*


@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewInboxByAccount(
        val acctname : String
) : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {

        val myAccount = accountService.accountInfo(acctname).single().state.data
        val criteria = QueryCriteria.VaultQueryCriteria(
                externalIds = listOf(myAccount.identifier.id)
        )

        val invoices = serviceHub.vaultService.queryBy(
                contractStateType = InvoiceState::class.java,
                criteria = criteria
        ).states.map { "\n" +"Invoice State : " +it.state.data.amount}

        return  invoices
    }
}



