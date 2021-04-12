package net.corda.samples.businessmembership.flows.membershipFlows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.bn.states.MembershipState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;

import java.util.List;

@StartableByRPC
public class QueryAllMembers extends FlowLogic<String> {

    @Override
    @Suspendable
    public String call() throws FlowException {

        List<StateAndRef<MembershipState>> membershipRequests  = getServiceHub().getVaultService().queryBy(MembershipState.class).getStates();
        String result = "\nQuery Found the following memberships:";
        for (StateAndRef<MembershipState> request : membershipRequests){
            result = result + "\n- [" +
                    request.getState().getData().getIdentity().getCordaIdentity().getName().getOrganisation()+
                    "] with membershipId: " + request.getState().getData().getLinearId() +
                    " | Membership Status: " + request.getState().getData().getStatus();
        }
        return result;
    }
}
