package com.tutorial.contracts;

import com.tutorial.states.AppleStamp;
import com.tutorial.states.TemplateState;
import net.corda.core.identity.Party;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class StateTests {

    //Mock State test check for if the state has correct parameters type
    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        TemplateState.class.getDeclaredField("msg");
        assertSame(String.class, TemplateState.class.getDeclaredField("msg").getType());
    }

    @Test
    public void AppleStampStateHasFieldOfCorrectType() throws NoSuchFieldException {
        AppleStamp.class.getDeclaredField("stampDesc");
        assertSame(String.class, AppleStamp.class.getDeclaredField("stampDesc").getType());

        AppleStamp.class.getDeclaredField("issuer");
        assertSame(Party.class, AppleStamp.class.getDeclaredField("issuer").getType());

        AppleStamp.class.getDeclaredField("holder");
        assertSame(Party.class, AppleStamp.class.getDeclaredField("issuer").getType());
    }

}