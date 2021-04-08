package net.corda.samples.businessmembership.contracts;

import net.corda.core.identity.Party;
import net.corda.samples.businessmembership.states.InsuranceState;
import org.junit.Test;

public class StateTests {

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        InsuranceState.class.getDeclaredField("insurer");
        // Is the message field of the correct type?
        assert(InsuranceState.class.getDeclaredField("insurer").getType().equals(Party.class));
    }
}