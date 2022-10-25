package net.corda.samples.multioutput.contracts;

import net.corda.samples.multioutput.states.TemplateState;
import org.junit.Test;

public class StateTests {

    //Mock State test check for if the state has correct parameters type
    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        TemplateState.class.getDeclaredField("msg");
        assert (TemplateState.class.getDeclaredField("msg").getType().equals(String.class));
    }
}