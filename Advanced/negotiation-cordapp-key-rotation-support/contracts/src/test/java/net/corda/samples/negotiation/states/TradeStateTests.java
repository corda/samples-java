package net.corda.samples.negotiation.states;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;


public class TradeStateTests {

    private int amount = 10;
    private final Party buyer = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();
    private final Party seller = new TestIdentity(new CordaX500Name("Charlie", "", "GB")).getParty();

    @Test
    public void constructorTest() {

        UniqueIdentifier tempId = new UniqueIdentifier();
        TradeState ts = new TradeState(amount, buyer, seller, tempId);

        assertEquals(ts.getAmount(), amount);
        assertEquals(ts.getBuyer(), buyer);
        assertEquals(ts.getSeller(), seller);
        assertEquals(ts.getLinearId(), tempId);

        assertNotEquals(ts.getAmount(), 0);
        assertNotEquals(ts.getAmount(), -5);
        assertNotEquals(ts.getAmount(), null);
    }

    @Test
    public void linearIdTest() {
        TradeState ts = new TradeState(amount, buyer, seller);

        // ensure ID is generated with shorter constructor stub
        assertTrue(ts.getLinearId() instanceof UniqueIdentifier);
    }

    @Test
    public void participantTest() {
        TradeState ts = new TradeState(amount, buyer, seller);

        // ensure participants are generated correctly
        assertTrue(ts.getParticipants().contains(buyer));
        assertTrue(ts.getParticipants().contains(seller));
    }

}
