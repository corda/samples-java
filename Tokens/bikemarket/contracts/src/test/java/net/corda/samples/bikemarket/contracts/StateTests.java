package net.corda.samples.bikemarket.contracts;

import net.corda.samples.bikemarket.states.WheelsTokenState;
import net.corda.testing.node.MockServices;
import org.junit.Test;
public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    //sample State tests
    @Test
    public void hasSerialNumFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        WheelsTokenState.class.getDeclaredField("serialNum");
        // Is the message field of the correct type?
        assert(WheelsTokenState.class.getDeclaredField("serialNum").getType().equals(String.class));
    }
}