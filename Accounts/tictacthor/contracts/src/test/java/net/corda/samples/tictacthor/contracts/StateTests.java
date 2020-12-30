package net.corda.samples.tictacthor.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.samples.tictacthor.states.BoardState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        BoardState.class.getDeclaredField("playerO");
        // Is the message field of the correct type?
        assert(BoardState.class.getDeclaredField("playerO").getType().equals(UniqueIdentifier.class));
    }
}