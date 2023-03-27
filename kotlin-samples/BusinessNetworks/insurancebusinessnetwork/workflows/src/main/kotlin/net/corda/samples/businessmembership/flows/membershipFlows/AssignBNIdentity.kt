package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.IllegalFlowArgumentException
import net.corda.bn.flows.ModifyBusinessIdentityFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.samples.businessmembership.states.CareProviderIdentity
import net.corda.samples.businessmembership.states.InsurerIdentity

@StartableByRPC
class AssignBNIdentity (
        private val firmType: String,
        private val membershipId: UniqueIdentifier,
        private val bnIdentity: String) : FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        if (this.firmType == "InsuranceFirm"){
            val insuranceIdentity = InsurerIdentity(bnIdentity).apply {
                if (!isValid()) {
                    throw IllegalFlowArgumentException("$bnIdentity in not a valid Insurance Identity")
                }
            }
            subFlow(ModifyBusinessIdentityFlow(membershipId, insuranceIdentity, notary))
        }else{
            val careProviderIdentity = CareProviderIdentity(bnIdentity).apply {
                if (!isValid()) {
                    throw IllegalFlowArgumentException("$bnIdentity in not a valid Care Provider Identity")
                }
            }
            subFlow(ModifyBusinessIdentityFlow(membershipId, careProviderIdentity, notary))
        }
        return "Issue a ${this.firmType} BN Identity to member(${this.membershipId})"
    }
}
//flow start AssignBNIdentity firmType: InsuranceFirm,
//membershipId: 55747c35-f761-4845-af11-acfc4639d6b9
//bnIdentity: APACIN76CZX



//flow start AssignBNIdentity firmType: InsuranceFirm, membershipId: 55747c35-f761-4845-af11-acfc4639d6b9, bnIdentity: APACIN76CZX
//flow start AssignBNIdentity firmType: CareProvider, membershipId: f3430cde-bbea-4c3e-8af3-9be86ee487ec, bnIdentity: APACCP44OJS

//run vaultQuery contractStateType: net.corda.bn.states.MembershipState