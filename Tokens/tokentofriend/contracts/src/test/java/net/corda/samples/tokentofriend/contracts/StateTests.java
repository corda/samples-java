package net.corda.samples.tokentofriend.contracts;

import net.corda.samples.tokentofriend.states.CustomTokenState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasMessageFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        CustomTokenState.class.getDeclaredField("message");
        // Is the message field of the correct type?
        assert(CustomTokenState.class.getDeclaredField("message").getType().equals(String.class));
    }
}