package net.corda.samples.secretsanta.contracts;

import net.corda.samples.secretsanta.states.SantaSessionState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SantaSessionStateTests {

    private List<String> playerNames = new ArrayList<>();
    private List<String> playerEmails = new ArrayList<>();

    private final Party santa = new TestIdentity(new CordaX500Name("Santa", "", "GB")).getParty();
    private final Party elf = new TestIdentity(new CordaX500Name("Elf", "", "GB")).getParty();

    @Before
    public void setup() {
        playerNames.add("david");
        playerNames.add("alice");
        playerNames.add("bob");
        playerNames.add("charlie");
        playerNames.add("olivia");
        playerNames.add("peter");

        playerEmails.add("david@corda.net");
        playerEmails.add("alice@corda.net");
        playerEmails.add("bob@corda.net");
        playerEmails.add("charlie@corda.net");
        playerEmails.add("olivia@corda.net");
        playerEmails.add("peter@corda.net");
    }

    @After
    public void tearDown() {
        // pass
    }

    // unmatching names
    @Test(expected = IllegalArgumentException.class)
    public void unmatchingNameTest() {
        ArrayList<String> badNames = new ArrayList<>(Arrays.asList("alice", "bob", "charlie", "olivia", "peter"));
        ArrayList<String> goodEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"));
        SantaSessionState st = new SantaSessionState(badNames, goodEmails, santa, elf);

    }

    // few name test
    @Test(expected = IllegalArgumentException.class)
    public void fewNameTest() {
        // here there are too few names
        ArrayList<String> badNames = new ArrayList<>(Arrays.asList("peter"));
        ArrayList<String> goodEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"));
        SantaSessionState st = new SantaSessionState(badNames, goodEmails, santa, elf);

    }

    // unmatching emails
    @Test(expected = IllegalArgumentException.class)
    public void unmatchingEmailTest() {
        // note there's no matching email for david, david@corda.net
        ArrayList<String> goodNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"));
        ArrayList<String> badEmails = new ArrayList<>(Arrays.asList("alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"));
        SantaSessionState st = new SantaSessionState(goodNames, badEmails, santa, elf);
    }

    // too few emails
    @Test(expected = IllegalArgumentException.class)
    public void fewEmailTest() {
        ArrayList<String> goodNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"));
        ArrayList<String> badEmails = new ArrayList<>(Arrays.asList("peter@corda.net"));
        SantaSessionState st = new SantaSessionState(goodNames, badEmails, santa, elf);
    }


    @Test(expected = IllegalArgumentException.class)
    public void tooFewPairingsTest() {
        SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa, elf);

        LinkedHashMap<String, String> assignments = new LinkedHashMap<>();
        assignments.put("david", "alice");
        assignments.put("alice", "bob");
        assignments.put("bob", "charlie");
        assignments.put("charlie", "olivia");
        assignments.put("olivia", "david");
        // note peter would be the "odd man out" and not have a valid assignment
        // assignments.put("peter", "peter");
        st.setAssignments(assignments);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidPairingsTest() {
        SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa, elf);

        LinkedHashMap<String, String> assignments = new LinkedHashMap<String, String>();
        assignments.put("david", "alice");
        assignments.put("alice", "bob");
        assignments.put("bob", "charlie");
        assignments.put("charlie", "olivia");
        assignments.put("olivia", "david");
        // note peter would be the "odd man out" and not have a valid assignment
        assignments.put("peter", "peter");

        st.setAssignments(assignments);
    }

    @Test
    public void stateGetters() {
        SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa, elf);

        assertEquals(santa, st.getIssuer());
        assertEquals(playerNames, st.getPlayerNames());
        assertEquals(playerEmails, st.getPlayerEmails());

        assertTrue(st.getPlayerNames().contains("olivia"));
        assertTrue(st.getPlayerNames().contains("peter"));

        assertTrue(st.getPlayerEmails().contains("olivia@corda.net"));
        assertTrue(st.getPlayerEmails().contains("peter@corda.net"));

        assertNotEquals(st.getAssignments().get("david"), st.getAssignments().get("peter"));
    }

    @Test
    public void stateImplementsContractState() {
        SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa, elf);
        assertTrue(st instanceof ContractState);
        assertTrue(st instanceof LinearState);
    }

    @Test
    public void stateHasOneParticipant() {
        SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa, elf);
        assertEquals(2, st.getParticipants().size());
        assertTrue(st.getParticipants().contains(santa));
        assertTrue(st.getParticipants().contains(elf));
    }

    @Test
    public void stateProducesValidAssignments() {
        SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa, elf);

        HashMap<String, String> assignments = st.getAssignments();
        // correct number of assignments
        assertEquals(playerNames.size(), assignments.size());
        // iterate through assignments for validity
        for (String santa_candidate: playerNames) {
            // ensure all these players actually exist
            assertTrue(playerNames.contains(santa_candidate));
            for (String target_candidate: playerNames) {
                // skip duplicates in iteration
                if (santa_candidate.equals(target_candidate)) { continue; }
                // ensure no one is assigned themselves
                assertNotEquals(santa_candidate, assignments.get(santa_candidate));
            }
        }
    }
}
