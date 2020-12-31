package net.corda.samples.auction.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.samples.auction.states.AuctionState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.UUID;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        AuctionState.class.getDeclaredField("auctionId");
        // Is the message field of the correct type?
        assert(AuctionState.class.getDeclaredField("auctionId").getType().equals(UUID.class));
    }
}