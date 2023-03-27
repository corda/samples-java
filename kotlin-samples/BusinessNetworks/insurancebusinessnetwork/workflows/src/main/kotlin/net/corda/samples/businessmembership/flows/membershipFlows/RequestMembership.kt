package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.RequestMembershipFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party

@StartableByRPC
class RequestMembership(val authorisedParty: Party, val networkId: String) : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        subFlow(RequestMembershipFlow(this.authorisedParty,this.networkId,null,null))
        return "\nRequest membership sent from [${ourIdentity.name.organisation}] nodes (ourself) to an authorized network member [${this.authorisedParty.name.organisation}]"
    }
}
//flow start RequestMembership authorisedParty: NetworkOperator, networkId: <xxxx-xxxx-xxx-xxx>