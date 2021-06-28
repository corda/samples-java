package net.corda.samples.election.states;

import net.corda.samples.election.contracts.VoteStateContract;
import net.corda.samples.election.contracts.VoteStateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
//import net.corda.samples.supplychain.contracts.PaymentStateContract;

import java.util.ArrayList;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(VoteStateContract.class)
public class VoteState implements ContractState {

    private int candidate;
    private AnonymousParty voter;
    //TODO Recipient is observer node not account
    private Party observer;
    private List<AbstractParty> participants;

    public VoteState(int candidate, AnonymousParty voter, Party observer) {
        this.candidate = candidate;
        this.voter = voter;
        this.observer = observer;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(voter);
        participants.add(observer);
    }

    public int getCandidate() {
        return candidate;
    }

    public void setCandidate(int candidate) {
        this.candidate = candidate;
    }

    public AnonymousParty getVoter() {
        return voter;
    }

    public void setVoter(AnonymousParty voter) {
        this.voter = voter;
    }

    public Party getObserver() {
        return observer;
    }

    public void setObserver(Party observer) {
        this.observer = observer;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
}