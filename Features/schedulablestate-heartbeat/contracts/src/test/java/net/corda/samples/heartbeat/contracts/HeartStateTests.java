package net.corda.samples.heartbeat.contracts;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.SchedulableState;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.heartbeat.states.HeartState;
import net.corda.testing.core.TestIdentity;
import org.junit.jupiter.api.Test;

import static org.jgroups.util.Util.assertFalse;
import static org.jgroups.util.Util.assertTrue;

public class HeartStateTests {

    private final TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));

    @Test
    public void constructorTest() {
        HeartState st = new HeartState(a.getParty());

        assertTrue(st.getParticipants().contains(a.getParty()));
        assertFalse(st.getParticipants().contains(b.getParty()));
    }

    @Test
    public void stateImplementsContractStateTest() {
        HeartState st = new HeartState(a.getParty());
        assertTrue(st instanceof ContractState);
        assertTrue(st instanceof SchedulableState);
    }
}



