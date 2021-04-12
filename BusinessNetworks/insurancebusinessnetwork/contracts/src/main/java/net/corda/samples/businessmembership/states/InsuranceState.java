package net.corda.samples.businessmembership.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.businessmembership.contracts.InsuranceStateContract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(InsuranceStateContract.class)
public class InsuranceState implements LinearState {
    private Party insurer;
    private String insuree;
    private Party careProvider;
    private String networkId;
    private String policyStatus;
    private UniqueIdentifier linearId;
    private List<AbstractParty> participants;

    @ConstructorForDeserialization
    public InsuranceState(Party insurer, String insuree, Party careProvider, String networkId, String policyStatus, UniqueIdentifier linearId, List<AbstractParty> participants) {
        this.insurer = insurer;
        this.insuree = insuree;
        this.careProvider = careProvider;
        this.networkId = networkId;
        this.policyStatus = policyStatus;
        this.linearId = linearId;
        this.participants = participants;
    }

    public InsuranceState(Party insurer, String insuree, Party careProvider, String networkId, String policyStatus) {
        this.insurer = insurer;
        this.insuree = insuree;
        this.careProvider = careProvider;
        this.networkId = networkId;
        this.policyStatus = policyStatus;

        this.linearId = new UniqueIdentifier();
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(insurer);
        this.participants.add(careProvider);
    }

    public Party getInsurer() {
        return insurer;
    }

    public String getInsuree() {
        return insuree;
    }

    public Party getCareProvider() {
        return careProvider;
    }

    public String getNetworkId() {
        return networkId;
    }

    public String getPolicyStatus() {
        return policyStatus;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearId;
    }
}
