package net.corda.samples.example.contracts;

import net.corda.samples.example.states.IOUState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        IOUState.class.getDeclaredField("value");
        // Is the message field of the correct type?
        assert(IOUState.class.getDeclaredField("value").getType().equals(Integer.class));
    }
}