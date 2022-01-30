package net.corda.samples.lending.states;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.lending.contracts.SyndicateContract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(SyndicateContract.class)
public class SyndicateState implements LinearState {

    private UniqueIdentifier uniqueIdentifier;
    private Party leadBank;
    private List<Party> participantBanks;
    private LinearPointer<ProjectState> projectDetails;
    private LinearPointer<LoanBidState> loanDetails;

    public SyndicateState(UniqueIdentifier uniqueIdentifier, Party leadBank, List<Party> participantBanks,
                          LinearPointer<ProjectState> projectDetails,
                          LinearPointer<LoanBidState> loanDetails) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.leadBank = leadBank;
        this.participantBanks = participantBanks;
        this.projectDetails = projectDetails;
        this.loanDetails = loanDetails;
    }

    public Party getLeadBank() {
        return leadBank;
    }

    public List<Party> getParticipantBanks() {
        return participantBanks;
    }

    public LinearPointer<ProjectState> getProjectDetails() {
        return projectDetails;
    }

    public LinearPointer<LoanBidState> getLoanDetails() {
        return loanDetails;
    }


    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> particiants = new ArrayList<>();
        particiants.add(leadBank);
        particiants.addAll(participantBanks);
        return particiants;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }
}
