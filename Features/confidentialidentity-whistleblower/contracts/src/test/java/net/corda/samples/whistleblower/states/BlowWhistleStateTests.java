package net.corda.samples.whistleblower.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;

public class BlowWhistleStateTests {

    private final TestIdentity a = new TestIdentity(new CordaX500Name("alice", "", "GB"));
    private final TestIdentity b = new TestIdentity(new CordaX500Name("bob", "", "GB"));
    private final TestIdentity c = new TestIdentity(new CordaX500Name("bad corp", "", "GB"));

    @Test
    public void constructorTest() {

        // here, c is the bad corporation, a is the Whistleblower, and b is the investigator
        BlowWhistleState st = new BlowWhistleState(c.getParty(), a.getParty().anonymise(), b.getParty().anonymise());

        assertEquals(a.getParty(), st.getWhistleBlower());
        assertEquals(c.getParty(), st.getBadCompany());
        assertEquals(b.getParty(), st.getInvestigator());

        assertTrue(st.getParticipants().contains(a.getParty()));
        assertTrue(st.getParticipants().contains(b.getParty()));
    }

    @Test
    public void stateImplementTests() {
        BlowWhistleState st = new BlowWhistleState(c.getParty(), a.getParty().anonymise(), b.getParty().anonymise());
        assertTrue(st instanceof ContractState);
        assertTrue(st instanceof LinearState);
        assertTrue(st.getLinearId() instanceof UniqueIdentifier);
    }
}
