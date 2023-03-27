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
import net.corda.samples.businessmembership.states.PolicyIssuerRole

@StartableByRPC
class AssignPolicyIssuerRole (
        private val membershipId: UniqueIdentifier,
        private val networkId: String) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val bnService = serviceHub.cordaService(BNService::class.java)
        val membershipState = bnService.getMembership(membershipId)?.state?.data?: throw MembershipNotFoundException("$ourIdentity is not member of Business Network with $networkId ID")
        return subFlow(ModifyRolesFlow(membershipId, membershipState.roles + PolicyIssuerRole(), notary))
    }
}
//flow start AssignPolicyIssuerRole membershipId: 55747c35-f761-4845-af11-acfc4639d6b9, networkId: 580104fc-4e83-431f-b4cf-95ec21ddc078