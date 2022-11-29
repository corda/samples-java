package net.corda.samples.referencestates.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

public class SanctionedEntitiesTests {


    TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    TestIdentity c = new TestIdentity(new CordaX500Name("Charlie", "", "GB"));

    @Test
    public void constructorTest() {

        List<Party> badPeople = Arrays.asList(a.getParty());
        Party issuer = b.getParty();


        SanctionedEntities st = new SanctionedEntities(badPeople, b.getParty());

        assertTrue(st instanceof ContractState);
        assertTrue(st instanceof LinearState);

        assertEquals(badPeople, st.getBadPeople());
        assertEquals(issuer, st.getIssuer());

        assertTrue(st.getParticipants().contains(b.getParty()));
        assertFalse(st.getParticipants().contains(a.getParty()));
        assertFalse(st.getParticipants().contains(c.getParty()));
    }

}


