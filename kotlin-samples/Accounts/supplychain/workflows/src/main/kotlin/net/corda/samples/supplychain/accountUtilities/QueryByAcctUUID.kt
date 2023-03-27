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

        val InternalMessages = serviceHub.vaultService.queryBy(
                contractStateType = InternalMessageState::class.java,
                criteria = criteria
        ).states.map { "\n" + "Internal Message: " + it.state.data.task }

        val payments = serviceHub.vaultService.queryBy(
                contractStateType = PaymentState::class.java,
                criteria = criteria
        ).states.map { "\n" +"Payment State : " +it.state.data.amount}

        val Cargos = serviceHub.vaultService.queryBy(
                contractStateType = CargoState::class.java,
                criteria = criteria
        ).states.map { "\n" +"Cargo State : " +it.state.data.cargo}

        val invoices = serviceHub.vaultService.queryBy(
                contractStateType = InvoiceState::class.java,
                criteria = criteria
        ).states.map { "\n" +"Invoice State : " +it.state.data.amount}

        val shippingRequest = serviceHub.vaultService.queryBy(
                contractStateType = ShippingRequestState::class.java,
                criteria = criteria
        ).states.map { "\n" +"Shipping Request State : " +it.state.data.cargo + " to " + it.state.data.DeliverTo}

        return InternalMessages + payments + Cargos + invoices + shippingRequest
    }
}



