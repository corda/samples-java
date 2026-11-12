package net.corda.samples.negotiation.states;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;


public class ProposalStateTests {

    private int amount = 10;
    private final Party proposer = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party buyer = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();
    private final Party seller = new TestIdentity(new CordaX500Name("Charlie", "", "GB")).getParty();
    private final Party proposee = new TestIdentity(new CordaX500Name("Dan", "", "GB")).getParty();

    @Test
    public void constructorTest() {

        UniqueIdentifier tempId = new UniqueIdentifier();
        ProposalState ps = new ProposalState(amount, buyer, seller, proposer, proposee, tempId);

        assertEquals(ps.getAmount(), amount);
        assertEquals(ps.getBuyer(), buyer);
        assertEquals(ps.getSeller(), seller);
        assertEquals(ps.getProposer(), proposer);
        assertEquals(ps.getProposee(), proposee);
        assertEquals(ps.getLinearId(), tempId);

        assertNotEquals(ps.getAmount(), 0);
        assertNotEquals(ps.getAmount(), -5);
        assertNotEquals(ps.getAmount(), null);
    }

    @Test
    public void linearIdTest() {
        ProposalState ps = new ProposalState(amount, buyer, seller, proposer, proposee);

        // ensure ID is generated with shorter constructor stub
        assertTrue(ps.getLinearId() instanceof UniqueIdentifier);
    }

    @Test
    public void participantTest() {
        ProposalState ps = new ProposalState(amount, buyer, seller, proposer, proposee);

        // ensure participants are generated correctly
        assertTrue(ps.getParticipants().contains(proposer));
        assertTrue(ps.getParticipants().contains(proposee));
        assertFalse(ps.getParticipants().contains(buyer));
        assertFalse(ps.getParticipants().contains(seller));
    }
}
