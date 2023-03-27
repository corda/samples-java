package net.corda.samples.supplychain.flows


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.supplychain.accountUtilities.NewKeyForAccount
import net.corda.samples.supplychain.contracts.InvoiceStateContract
import net.corda.samples.supplychain.states.InvoiceState

@StartableByRPC
@StartableByService
@InitiatingFlow
class SendToNonParticipantAcct(
        val whoAmI: String,
        val whereTo:String,
        val stateId:UUID
) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {

        val targetAccount = accountService.accountInfo(whereTo).single().state.data

        val outputStateAndRef: StateAndRef<*> = serviceHub.vaultService.queryBy<InvoiceState>(InvoiceState::class.java)
            .states.filter { it.state.data.invoiceID == stateId }[0]
        accountService.shareStateWithAccount(targetAccount.identifier.id,outputStateAndRef)

        return "Invoice shared to " + targetAccount.host.name.organisation + "'s "+ targetAccount.name + " team."
    }
}

//run vaultQuery contractStateType: net.corda.samples.supplychain.states.InvoiceState
//flow start SendToNonParticipantAcct whoAmI: BuyerProcurement, whereTo: BuyerFinance, stateId:
//flow start SendToNonParticipantAcct whoAmI: SellerSales, whereTo: BuyerFinance, stateId: 3599f97c-96dd-4374-99a7-bd4ef3727014