package net.corda.samples.referencestates.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;

public class SanctionableIOUStateTests {

    TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));

    @Test
    public void constructorTest() {

        int value = 50;
        Party lender = a.getParty();
        Party borrower = b.getParty();
        UniqueIdentifier uid = new UniqueIdentifier();

        SanctionableIOUState st = new SanctionableIOUState(value, lender, borrower, uid);

        assertTrue(st instanceof ContractState);
        assertTrue(st instanceof LinearState);

        assertEquals(value, st.getValue());
        assertEquals(lender, st.getLender());
        assertEquals(borrower, st.getBorrower());
        assertEquals(uid, st.getLinearId());

        assertTrue(st.getParticipants().contains(a.getParty()));
        assertTrue(st.getParticipants().contains(b.getParty()));

    }

}

