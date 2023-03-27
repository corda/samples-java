package net.corda.samples.businessmembership.flows.membershipFlows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.bn.flows.ModifyBusinessIdentityFlow;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.samples.businessmembership.states.CareProviderIdentity;
import net.corda.samples.businessmembership.states.InsurerIdentity;

@StartableByRPC
public class AssignBNIdentity extends FlowLogic<String> {

    private String firmType;
    private UniqueIdentifier membershipId;
    private String bnIdentity;

    public AssignBNIdentity(String firmType, UniqueIdentifier membershipId, String bnIdentity) {
        this.firmType = firmType;
        this.membershipId = membershipId;
        this.bnIdentity = bnIdentity;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        // Obtain a reference to a notary we wish to use.
        /** Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)*/
        final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
        if(this.firmType.equals("InsuranceFirm")){
            InsurerIdentity insuranceIdty = new InsurerIdentity(bnIdentity);
            if(!insuranceIdty.isValid()){
                throw new IllegalArgumentException(""+bnIdentity+" in not a valid Insurance Identity");
            }
            subFlow(new ModifyBusinessIdentityFlow(membershipId, insuranceIdty, notary));
        }else{
            CareProviderIdentity careProviderIdty = new CareProviderIdentity(bnIdentity);
            if(!careProviderIdty.isValid()){
                throw new IllegalArgumentException(""+bnIdentity+" in not a valid Insurance Identity");
            }
            subFlow(new ModifyBusinessIdentityFlow(membershipId, careProviderIdty, notary));
        }
        return "Issue a "+ this.firmType+" BN Identity to member("+ this.membershipId+")";
    }
}
