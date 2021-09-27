package com.tutorial.contracts;

import com.tutorial.states.AppleStamp;
import com.tutorial.states.TemplateState;
import net.corda.core.identity.Party;
import org.junit.Test;

public class StateTests {

    //Mock State test check for if the state has correct parameters type
    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        TemplateState.class.getDeclaredField("msg");
        assert (TemplateState.class.getDeclaredField("msg").getType().equals(String.class));
    }

    @Test
    public void AppleStampStateHasFieldOfCorrectType() throws NoSuchFieldException {
        AppleStamp.class.getDeclaredField("stampDesc");
        assert (AppleStamp.class.getDeclaredField("stampDesc").getType().equals(String.class));

        AppleStamp.class.getDeclaredField("issuer");
        assert (AppleStamp.class.getDeclaredField("issuer").getType().equals(Party.class));

        AppleStamp.class.getDeclaredField("holder");
        assert (AppleStamp.class.getDeclaredField("issuer").getType().equals(Party.class));
    }

}