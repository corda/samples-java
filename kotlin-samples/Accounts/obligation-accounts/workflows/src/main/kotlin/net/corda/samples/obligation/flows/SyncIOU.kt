package net.corda.samples.obligation.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.samples.obligation.states.IOUState

@InitiatingFlow
@StartableByRPC
class SyncIOU(
    val linearId: UniqueIdentifier,
    private val party: Party
) : FlowLogic<String>() {

    @Suspendable
    override fun call():String {

        val iouToSettle = serviceHub.vaultService.queryBy(
            contractStateType = IOUState::class.java
        ).states.filter {it.state.data.linearId == linearId}[0]

        subFlow(ShareStateAndSyncAccounts(iouToSettle,party))
        return "IOU synced"
    }
}