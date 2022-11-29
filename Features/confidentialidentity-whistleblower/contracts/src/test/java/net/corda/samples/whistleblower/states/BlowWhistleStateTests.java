package net.corda.samples.whistleblower.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;
import org.junit.Assert;


public class BlowWhistleStateTests {

    private final TestIdentity a = new TestIdentity(new CordaX500Name("alice", "", "GB"));
    private final TestIdentity b = new TestIdentity(new CordaX500Name("bob", "", "GB"));
    private final TestIdentity c = new TestIdentity(new CordaX500Name("bad corp", "", "GB"));

    @Test
    public void constructorTest() {

        // here, c is the bad corporation, a is the Whistleblower, and b is the investigator
        BlowWhistleState st = new BlowWhistleState(c.getParty(), a.getParty().anonymise(), b.getParty().anonymise());

        Assert.assertEquals(a.getParty(), st.getWhistleBlower());
        Assert.assertEquals(c.getParty(), st.getBadCompany());
        Assert.assertEquals(b.getParty(), st.getInvestigator());

        Assert.assertTrue(st.getParticipants().contains(a.getParty()));
        Assert.assertTrue(st.getParticipants().contains(b.getParty()));
    }

    @Test
    public void stateImplementTests() {
        BlowWhistleState st = new BlowWhistleState(c.getParty(), a.getParty().anonymise(), b.getParty().anonymise());
        Assert.assertTrue(st instanceof ContractState);
        Assert.assertTrue(st instanceof LinearState);
        Assert.assertTrue(st.getLinearId() instanceof UniqueIdentifier);
    }
}
