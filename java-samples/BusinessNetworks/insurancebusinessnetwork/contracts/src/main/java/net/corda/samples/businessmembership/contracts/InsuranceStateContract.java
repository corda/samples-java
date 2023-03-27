package net.corda.samples.businessmembership.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.businessmembership.states.CareProviderIdentity;
import net.corda.samples.businessmembership.states.InsuranceState;
import net.corda.samples.businessmembership.states.InsurerIdentity;
import org.jetbrains.annotations.NotNull;
import net.corda.bn.states.MembershipState;
import net.corda.core.identity.Party;

import java.lang.IllegalArgumentException;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class InsuranceStateContract implements Contract {

    public static final String InsuranceStateContract_ID = "net.corda.samples.businessmembership.contracts.InsuranceStateContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandData command = tx.getCommands().get(0).getValue();
        InsuranceState output = (InsuranceState) tx.getOutputs().get(0).getData();
        if (command instanceof Commands.Issue){
            verifyIssue(tx,output.getNetworkId(), output.getInsurer(), output.getCareProvider());
        }else{
            throw new IllegalArgumentException("Unsupported command "+command);
        }
    }

    public void verifyIssue(LedgerTransaction tx, String networkId, Party insurance, Party careProvider){
        verifyMembershipsForMedInsuranceTransaction(tx, networkId, insurance, careProvider, "Issue");

    }
    public void verifyMembershipsForMedInsuranceTransaction(LedgerTransaction tx, String networkId,
                                                            Party insurance, Party careProvider,String commandName){
        requireThat(require -> {
            //Verify number of memberships
            require.using("Insurance "+ commandName+" transaction should have 2 reference states", tx.getReferences().size() == 2);
            require.using("Insurance "+ commandName+" transaction should contain only reference MembershipStates",
                    tx.getReferenceStates().stream().allMatch(it -> it.getClass() == MembershipState.class));

            //Extract memberships
            List<MembershipState> membershipReferenceStates = tx.getReferenceStates().stream().map( it -> (MembershipState) it).collect(Collectors.toList());
            require.using("Insurance "+ commandName+
                    " transaction should contain only reference membership states from Business Network with "+networkId+" ID",
                    membershipReferenceStates.stream().allMatch(it -> it.getNetworkId().equals(networkId)));

            //Extract Membership and verify not null
            MembershipState insuranceMembership = membershipReferenceStates.stream()
                    .filter(it -> (it.getNetworkId().equals(networkId) && it.getIdentity().getCordaIdentity().equals(insurance)))
                    .collect(Collectors.toList()).get(0);
            require.using("\nInsurance "+ commandName+" transaction should have insurance's reference membership state", insuranceMembership!= null);

            MembershipState careProviderMembership = membershipReferenceStates.stream()
                    .filter(it -> (it.getNetworkId().equals(networkId) && it.getIdentity().getCordaIdentity().equals(careProvider)))
                    .collect(Collectors.toList()).get(0);
            require.using("\nInsurance "+ commandName+" transaction should have careProvider's reference membership state", careProviderMembership!= null);

            //Exam the customized Identity
            require.using("Insurance should be active member of Business Network with "+networkId, insuranceMembership.isActive());
            require.using("insurance should have business identity of FirmIdentity type",
                    insuranceMembership.getIdentity().getBusinessIdentity().getClass().equals(InsurerIdentity.class));

            require.using("careProvider should be active member of Business Network with "+networkId, careProviderMembership.isActive());
            require.using("insurance should have business identity of FirmIdentity type",
                    careProviderMembership.getIdentity().getBusinessIdentity().getClass().equals(CareProviderIdentity.class));
            return null;
        });
    }


    public interface Commands extends CommandData{
        class Issue implements Commands {}
        class Claim implements Commands {}

    }
}
