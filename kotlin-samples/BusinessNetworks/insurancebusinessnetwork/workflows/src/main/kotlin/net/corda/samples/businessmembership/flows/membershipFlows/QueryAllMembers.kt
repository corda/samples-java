package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.states.MembershipState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class QueryAllMembers : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {
        val membershipRequests = serviceHub.vaultService.queryBy(contractStateType = MembershipState::class.java).states
        return "\nQuery Found the following memberships:" +
                membershipRequests.map { "\n- [" +
                        it.state.data.identity.cordaIdentity.name.organisation +
                        "] with membershipId: " + it.state.data.linearId +
                        " | Membership Status: " + it.state.data.status}
    }
}

//flow start QueryAllMembers