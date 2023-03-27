package net.corda.samples.obligation.accountUtil


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow


@StartableByRPC
@StartableByService
@InitiatingFlow
class CreateNewAccountAndShare(
    private val acctName:String,
    private val node2 : Party,
    private val node3 : Party) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        //Create a new account
        val newAccount = accountService.createAccount(name = acctName).toCompletableFuture().getOrThrow()
        val acct = newAccount.state.data

        accountService.shareAccountInfoWithParty(acct.identifier.id, node2)
        accountService.shareAccountInfoWithParty(acct.identifier.id, node3)

        return ""+acct.name + " team's account was created. UUID is : " + acct.identifier
    }
}

//flow start CreateNewAccountAndShare acctName: bob6424, node2: ParticipantB, node3: ParticipantC
//flow start CreateNewAccountAndShare acctName: Julie7465, node2: ParticipantA, node3: ParticipantC
//flow start CreateNewAccountAndShare acctName: Peter7548, node2: ParticipantB, node3: ParticipantA
