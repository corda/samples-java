package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.BNService
import net.corda.bn.flows.MembershipNotFoundException
import net.corda.bn.flows.ModifyRolesFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.samples.businessmembership.states.OrthodonticsRole

@StartableByRPC
class AssignOrthodonticsRole(
        private val membershipId: UniqueIdentifier,
        private val networkId: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val bnService = serviceHub.cordaService(BNService::class.java)
        val membershipState = bnService.getMembership(membershipId)?.state?.data?: throw MembershipNotFoundException("$ourIdentity is not member of Business Network with $networkId ID")
        return subFlow(ModifyRolesFlow(membershipId, membershipState.roles + OrthodonticsRole(), notary))
    }
}
//flow start AssignOrthodonticsPermissions membershipId: f3430cde-bbea-4c3e-8af3-9be86ee487ec,
//networkId: 580104fc-4e83-431f-b4cf-95ec21ddc078

//flow start AssignOrthodonticsPermissions membershipId: f3430cde-bbea-4c3e-8af3-9be86ee487ec, networkId: 580104fc-4e83-431f-b4cf-95ec21ddc078