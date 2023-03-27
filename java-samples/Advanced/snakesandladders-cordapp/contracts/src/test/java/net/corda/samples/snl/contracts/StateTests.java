package net.corda.samples.snl.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.samples.snl.states.GameBoard;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        GameBoard.class.getDeclaredField("linearId");
        // Is the message field of the correct type?
        assert(GameBoard.class.getDeclaredField("linearId").getType().equals(UniqueIdentifier.class));
    }
}