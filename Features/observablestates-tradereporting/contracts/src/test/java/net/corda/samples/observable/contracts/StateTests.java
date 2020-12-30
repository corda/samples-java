package net.corda.samples.observable.contracts;

import net.corda.core.identity.Party;
import net.corda.samples.observable.states.HighlyRegulatedState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        HighlyRegulatedState.class.getDeclaredField("buyer");
        // Is the message field of the correct type?
        assert(HighlyRegulatedState.class.getDeclaredField("buyer").getType().equals(Party.class));
    }
}