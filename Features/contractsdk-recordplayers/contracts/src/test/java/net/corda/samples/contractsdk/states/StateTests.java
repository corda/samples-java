package net.corda.samples.contractsdk.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    // in this example, bob is intended to be the dealer and record maintenance service.
    private final Party alice = new TestIdentity(new CordaX500Name("Alice Audio", "", "GB")).getParty();
    private final Party bob = new TestIdentity(new CordaX500Name("Bob's Hustle Records", "", "GB")).getParty();

    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
        // pass
    }

    // unmatching names
    @Test(expected = IllegalArgumentException.class)
    public void invalidMagneticStrength() {
        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, -5, 700, 10000, 0, new UniqueIdentifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSongsPlayed() {
        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, -200, new UniqueIdentifier());
    }

    @Test
    public void stateImplementsContractState() {
        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL);
        assertTrue(st instanceof ContractState);
        assertTrue(st instanceof LinearState);
        assertTrue(st.getUid() instanceof UniqueIdentifier);
    }

    @Test
    public void constructorTests() {
        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        assertEquals(alice, st.getManufacturer());
        assertEquals(bob, st.getDealer());
        assertEquals(100, st.getMagneticStrength());
        assertEquals(700, st.getCoilTurns());
        assertEquals(10000, st.getAmplifierSNR());
        assertEquals(0, st.getSongsPlayed());
    }

    @Test
    public void updateMethodTests() {

        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        assertEquals(100, st.getMagneticStrength());
        assertEquals(700, st.getCoilTurns());
        assertEquals(10000, st.getAmplifierSNR());

        // change params
        assertEquals(50, st.update(st.getNeedle(), 50, 650, 8000, st.getSongsPlayed()).getMagneticStrength());
        assertEquals(650, st.update(st.getNeedle(), 50, 650, 8000, st.getSongsPlayed()).getCoilTurns());
        assertEquals(8000, st.update(st.getNeedle(), 50, 650, 8000, st.getSongsPlayed()).getAmplifierSNR());

    }


}
