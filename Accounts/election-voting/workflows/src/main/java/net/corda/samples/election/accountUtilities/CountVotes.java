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

//    private final String acctName;

//    public CountVotes(String acctname) {
    public CountVotes() {
    }

    @Override
    public List<Integer> call() throws FlowException {

        List<Integer> voteCounts = new ArrayList<Integer>(Collections.nCopies(10,0));
        List<StateAndRef<VoteState>> votes = getServiceHub().getVaultService().queryBy(VoteState.class).getStates();
        for (StateAndRef<VoteState> vote : votes) {
            VoteState recordedVote = vote.getState().getData();
//            System.out.println("\nRECORDED VOTE DATA: " + recordedVote);
            int recordedVoteChoice = recordedVote.getChoice();
            voteCounts.set(recordedVoteChoice, voteCounts.get(recordedVoteChoice) + 1);
        }

        return voteCounts;
    }
}