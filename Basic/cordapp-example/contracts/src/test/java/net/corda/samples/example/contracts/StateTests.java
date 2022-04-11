package net.corda.samples.example.contracts;

import net.corda.samples.example.states.IOUState;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class StateTests {
    @Test
    public void hasAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        IOUState.class.getDeclaredField("value");
        // Is the message field of the correct type?
        assertSame(IOUState.class.getDeclaredField("value").getType(), Integer.class);
    }
}
