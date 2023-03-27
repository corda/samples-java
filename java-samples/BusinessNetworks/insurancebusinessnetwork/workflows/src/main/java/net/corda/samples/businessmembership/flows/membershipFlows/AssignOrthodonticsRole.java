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
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.businessmembership.states.CareProviderIdentity;

import java.util.HashSet;
import java.util.Set;

@StartableByRPC
public class AssignOrthodonticsRole extends FlowLogic<SignedTransaction> {

    private UniqueIdentifier membershipId;
    private String networkId;

    public AssignOrthodonticsRole(UniqueIdentifier membershipId, String networkId) {
        this.membershipId = membershipId;
        this.networkId = networkId;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
        BNService bnService = getServiceHub().cordaService(BNService.class);
        MembershipState membershipState = bnService.getMembership(membershipId).getState().getData();
        Set<BNRole> roles = new HashSet<>();
        for(BNRole br : membershipState.getRoles()){
            roles.add(br);
        }
        roles.add(new CareProviderIdentity.OrthodonticsRole());
        return subFlow(new ModifyRolesFlow(this.membershipId,roles,notary));
    }
}
