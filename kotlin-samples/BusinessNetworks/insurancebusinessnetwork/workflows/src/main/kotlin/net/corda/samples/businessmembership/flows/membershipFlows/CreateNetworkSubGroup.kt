package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.CreateGroupFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name

@StartableByRPC
class CreateNetworkSubGroup(private val networkId: String,
                            private val groupName: String,
                            private val groupParticipants: Set<UniqueIdentifier> = emptySet()
) : FlowLogic<String>(){
    @Suspendable
    override fun call(): String {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val groupId = UniqueIdentifier()
        subFlow(CreateGroupFlow(this.networkId,groupId,this.groupName,this.groupParticipants,notary))
        return "\n${this.groupName} has created under BN network (${this.networkId})"+
                "GroupId: $groupId"+
                "\nAdded participants(shown by membershipId): ${this.groupParticipants.map { "\n- $it" }}"
    }
}
//flow start CreateNetworkSubGroup networkId: 580104fc-4e83-431f-b4cf-95ec21ddc078,
//groupName: APAC_Insurance_Alliance,
//groupParticipants: [f3430cde-bbea-4c3e-8af3-9be86ee487ec, 55747c35-f761-4845-af11-acfc4639d6b9]

//flow start CreateNetworkSubGroup networkId: 580104fc-4e83-431f-b4cf-95ec21ddc078, groupName: APAC_Insurance_Alliance, groupParticipants: [f3430cde-bbea-4c3e-8af3-9be86ee487ec, 55747c35-f761-4845-af11-acfc4639d6b9]