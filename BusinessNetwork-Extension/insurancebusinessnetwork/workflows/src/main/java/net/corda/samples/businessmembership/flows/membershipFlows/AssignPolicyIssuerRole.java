package net.corda.samples.businessmembership.flows.membershipFlows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.bn.flows.BNService;
import net.corda.bn.flows.ModifyRolesFlow;
import net.corda.bn.states.BNRole;
import net.corda.bn.states.MembershipState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.businessmembership.states.InsurerIdentity;

import java.util.HashSet;
import java.util.Set;

@StartableByRPC
public class AssignPolicyIssuerRole extends FlowLogic<SignedTransaction> {

    private UniqueIdentifier membershipId;
    private String networkId;

    public AssignPolicyIssuerRole(UniqueIdentifier membershipId, String networkId) {
        this.membershipId = membershipId;
        this.networkId = networkId;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        BNService bnService = getServiceHub().cordaService(BNService.class);
        MembershipState membershipState = bnService.getMembership(this.membershipId).getState().getData();
        Set<BNRole> roles = new HashSet<>();
        for(BNRole br : membershipState.getRoles()){
            roles.add(br);
        }
        roles.add(new InsurerIdentity.PolicyIssuerRole());
        return subFlow(new ModifyRolesFlow(membershipId, roles, notary));
    }
}
