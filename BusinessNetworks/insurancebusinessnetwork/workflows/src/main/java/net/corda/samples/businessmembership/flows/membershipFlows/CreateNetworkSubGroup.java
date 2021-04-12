package net.corda.samples.businessmembership.flows.membershipFlows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.bn.flows.CreateGroupFlow;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

import java.util.Set;

@StartableByRPC
public class CreateNetworkSubGroup extends FlowLogic<String> {

    private String networkId;
    private String groupName;
    private Set<UniqueIdentifier> groupParticipants;

    public CreateNetworkSubGroup(String networkId, String groupName, Set<UniqueIdentifier> groupParticipants) {
        this.networkId = networkId;
        this.groupName = groupName;
        this.groupParticipants = groupParticipants;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        UniqueIdentifier groupId = new UniqueIdentifier();
        subFlow(new CreateGroupFlow(this.networkId,groupId,this.groupName,this.groupParticipants,notary));
        String result = "\n "+ this.groupName+ " has created under BN network ("+this.networkId+")"+
                "GroupId: "+groupId.toString();
        for (UniqueIdentifier id: groupParticipants){
            result = result + "\nAdded participants(shown by membershipId): "+ id.toString();
        }
        return result;
    }
}
