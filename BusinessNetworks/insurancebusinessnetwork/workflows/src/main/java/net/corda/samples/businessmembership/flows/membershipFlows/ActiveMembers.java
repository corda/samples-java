package net.corda.samples.businessmembership.flows.membershipFlows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.bn.flows.ActivateMembershipFlow;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

@StartableByRPC
public class ActiveMembers extends FlowLogic<String> {

    private UniqueIdentifier membershipId;

    public ActiveMembers(UniqueIdentifier membershipId) {
        this.membershipId = membershipId;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        subFlow(new ActivateMembershipFlow(this.membershipId,notary));
        return "\nMember("+ this.membershipId.toString()+")'s network membership has been activated.";
    }
}
