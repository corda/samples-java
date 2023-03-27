package net.corda.samples.obligation.accountUtil



import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.finance.contracts.asset.Cash
import net.corda.samples.obligation.states.IOUState


@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewAccounts() : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {
        //Create a new account
        val aAccountsQuery = accountService.allAccounts().map {"\n"+it.state.data.name + " from host[" + it.state.data.host+ "]" + ", id: "+ it.state.data.identifier.id}
        return aAccountsQuery
    }
}

@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewCashBalanceByAccount(
    val acctname : String
) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        val myAccount = accountService.accountInfo(acctname).single().state.data
        val criteria = QueryCriteria.VaultQueryCriteria(
            externalIds = listOf(myAccount.identifier.id)
        )
        var totalBalance :Long = 0
        val cash = serviceHub.vaultService.queryBy(
            contractStateType = Cash.State::class.java,
            criteria = criteria
        ).states.map {
            totalBalance += it.state.data.amount.quantity
            "\n" + "cash balance: " + it.state.data.amount
        }
        totalBalance /= 100
        return "\nTotal Balance : $totalBalance USD.\nEach money drop is: $cash"
    }

}

@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewIOUByAccount(
    val acctname : String
) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        val myAccount = accountService.accountInfo(acctname).single().state.data
        val name =myAccount.name
        val criteria = QueryCriteria.VaultQueryCriteria(
            externalIds = listOf(myAccount.identifier.id)
        )
        val ious = serviceHub.vaultService.queryBy(
            contractStateType = IOUState::class.java,
            criteria = criteria
        ).states.map {
            val lenderAccount = accountService.accountInfo(it.state.data.lenderAcctID)!!.state.data

            "\n" + name + " borrowed "+ it.state.data.amount +" from acct: " + lenderAccount.name +"(Node: "+ lenderAccount.host.name.organisation+") | Paid : " + it.state.data.paid
        }
        return "\n$name has iou is: $ious"
    }

}

//flow start ViewAccounts
//flow start IOUIssueFlow meID: 24e45cb4-4473-4420-8064-ad5128ccef53, lenderID: cb1e1f55-3c8a-48bb-aee6-6ceed8605cb2, amount: 20

//flow start MoneyDropFlow acctID: 24e45cb4-4473-4420-8064-ad5128ccef53
//flow start ViewIOUByAccount acctname: bob6424
//flow start ViewCashBalanceByAccount acctname: bob6424


//flow start IOUSettleFlow linearId: 53c4cc93-5c0d-403f-9cf4-7770583de61b, meID: 24e45cb4-4473-4420-8064-ad5128ccef53, settleAmount: 5
//flow start IOUTransferFlow linearId: 53c4cc93-5c0d-403f-9cf4-7770583de61b, meID: cb1e1f55-3c8a-48bb-aee6-6ceed8605cb2, newLenderID: 582aa5d9-fcc6-4cb1-94b7-95c525f45a3c

//flow start SyncIOU linearId: 53c4cc93-5c0d-403f-9cf4-7770583de61b, party: ParticipantA
//flow start IOUSettleFlow linearId: 53c4cc93-5c0d-403f-9cf4-7770583de61b, meID: 24e45cb4-4473-4420-8064-ad5128ccef53, settleAmount: 5
