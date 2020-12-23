package net.corda.samples.heartbeat.contracts;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.SchedulableState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.samples.heartbeat.states.HeartState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;
import static org.jgroups.util.Util.assertFalse;
import static org.jgroups.util.Util.assertTrue;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        HeartState.class.getDeclaredField("me");
        assert(HeartState.class.getDeclaredField("me").getType().equals(Party.class));
    }
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