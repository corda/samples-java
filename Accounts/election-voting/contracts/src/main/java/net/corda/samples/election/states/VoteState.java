package net.corda.samples.election.states;

import net.corda.samples.election.contracts.VoteStateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.ArrayList;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(VoteStateContract.class)
public class VoteState implements ContractState {

    private int choice;
    private String voter;
    private Party observer;
    private String opportunity;
    private final List<AbstractParty> participants;

    public VoteState(int choice, String voter, Party observer, String opportunity) {
        this.choice = choice;
        this.voter = voter;
        this.observer = observer;
        this.opportunity = opportunity;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(observer);
    }

    public int getChoice() {
        return choice;
    }

    public void setChoice(int choice) {
        this.choice = choice;
    }

    public String getVoter() {
        return voter;
    }

    public void setVoter(String voter) {
        this.voter = voter;
    }

    public Party getObserver() {
        return observer;
    }

    public void setObserver(Party observer) {
        this.observer = observer;
    }

    public String getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(String opportunity) {
        this.opportunity = opportunity;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
}