package net.corda.samples.lending.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.lending.contracts.SyndicateBidContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(SyndicateBidContract.class)
public class SyndicateBidState implements LinearState {

    private UniqueIdentifier uniqueIdentifier;
    private LinearPointer<SyndicateState> syndicateState;
    private int bidAmount;
    private Party leadBank;
    private Party participatBank;
    private String status;

    public SyndicateBidState(UniqueIdentifier uniqueIdentifier, LinearPointer<SyndicateState> syndicateState, int bidAmount,
                             Party leadBank, Party participatBank, String status) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.syndicateState = syndicateState;
        this.bidAmount = bidAmount;
        this.leadBank = leadBank;
        this.participatBank = participatBank;
        this.status = status;
    }

    public LinearPointer<SyndicateState> getSyndicateState() {
        return syndicateState;
    }

    public int getBidAmount() {
        return bidAmount;
    }

    public Party getLeadBank() {
        return leadBank;
    }

    public Party getParticipatBank() {
        return participatBank;
    }

    public String getStatus() {
        return status;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(participatBank, leadBank);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }
}
