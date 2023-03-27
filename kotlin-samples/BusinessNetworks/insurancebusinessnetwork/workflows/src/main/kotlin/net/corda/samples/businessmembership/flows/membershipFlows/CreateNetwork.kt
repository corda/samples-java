package net.corda.samples.businessmembership.flows.membershipFlows

import co.paralleluniverse.fibers.Suspendable
import net.corda.bn.flows.CreateBusinessNetworkFlow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class CreateNetwork: FlowLogic<String>() {
    @Suspendable
    override fun call(): String {
        val networkId = UniqueIdentifier()
        subFlow(CreateBusinessNetworkFlow(networkId = networkId))
        return "\nA network was created with NetworkID: $networkId"
    }
}
//flow start CreateNetwork