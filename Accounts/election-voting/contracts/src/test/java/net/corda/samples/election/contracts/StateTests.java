package net.corda.samples.election.contracts;

import net.corda.samples.election.states.VoteState;
import org.junit.Test;

public class StateTests {

    //Mock State test check for if the state has correct parameters type
    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        VoteState.class.getDeclaredField("candidate");
        assert (VoteState.class.getDeclaredField("candidate").getType().equals(int.class));
    }
}