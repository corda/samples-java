package net.corda.samples.election.accountUtilities;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.flows.StartableByService;
import net.corda.samples.election.states.VoteState;

import java.util.*;

@StartableByRPC
@StartableByService
public class CountVotes extends FlowLogic<List<Integer>> {

    private final String opportunity;

    public CountVotes(String opportunity) {
        this.opportunity = opportunity;
    }

    @Override
    public List<Integer> call() throws FlowException {

        List<Integer> voteCounts = new ArrayList<Integer>(Collections.nCopies(10,0));
        List<StateAndRef<VoteState>> votes = getServiceHub().getVaultService().queryBy(VoteState.class).getStates();
        HashMap<String, Integer> voteMap = new HashMap<>();
        for (StateAndRef<VoteState> vote : votes) {
            VoteState recordedVote = vote.getState().getData();
            if (recordedVote.getOpportunity().equals(opportunity)) {
                voteMap.put(recordedVote.getVoter(), recordedVote.getChoice());
            }
        }
        for (Integer i : voteMap.values()) {
            voteCounts.set(i, voteCounts.get(i) + 1);
        }
        return voteCounts;
    }
}