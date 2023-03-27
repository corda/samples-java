package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.ActivateMembershipFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name

@StartableByRPC
class ActiveMembers(val membershipId: UniqueIdentifier): FlowLogic<String>()  {

    @Suspendable
    override fun call(): String {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        subFlow(ActivateMembershipFlow(this.membershipId,notary))
        return "\nMember(${this.membershipId})'s network membership has been activated."
    }
}
//flow start ActiveMembers membershipId: