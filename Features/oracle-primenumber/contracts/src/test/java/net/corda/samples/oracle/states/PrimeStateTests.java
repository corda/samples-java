package net.corda.samples.oracle.states;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;

public class PrimeStateTests {

    TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));

    @Test
    public void constructorTest() {
        PrimeState st = new PrimeState(1, 5, a.getParty());

        assertEquals(a.getParty(), st.getRequester());
        assertTrue(st.getParticipants().contains(a.getParty()));
    }
}
