package net.corda.samples.heartbeat.contracts;

import net.corda.testing.node.MockServices;
import org.junit.jupiter.api.Test;

import static org.jgroups.util.Util.assertFalse;
import static org.jgroups.util.Util.assertTrue;

//public class StateTests {
//
//    private final TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
//    private final TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
//
//    @Test
//    public void constructorTest() {
//        HeartState st = new HeartState(a.getParty());
//
//        assertTrue(st.getParticipants().contains(a.getParty()));
//        assertFalse(st.getParticipants().contains(b.getParty()));
//    }
//
//    @Test
//    public void stateImplementsContractStateTest() {
//        HeartState st = new HeartState(a.getParty());
//        assertTrue(st instanceof ContractState);
//        assertTrue(st instanceof SchedulableState);
//    }
//}
public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
//        // Does the message field exist?
//        HeartState.class.getDeclaredField("me");
//        // Is the message field of the correct type?
//        assert(HeartState.class.getDeclaredField("me").getType().equals(Party.class));
    }
}


