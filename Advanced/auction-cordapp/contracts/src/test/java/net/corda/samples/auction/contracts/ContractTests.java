package net.corda.samples.auction.contracts;

import net.corda.core.contracts.Amount;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.auction.states.AuctionState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class ContractTests {
    private final TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "GB")));


    @Test
    public void testVerifyBid() {
        AuctionState input = new AuctionState(null, null, Amount.parseCurrency("100 USD"),
                Amount.parseCurrency("200 USD"), null, null, null, true, null,
                null, null);


        AuctionState input_inactive = new AuctionState(null, null, Amount.parseCurrency("100 USD"),
                Amount.parseCurrency("200 USD"), null, null, null, false, null,
                null, null);

        AuctionState output = new AuctionState(null, null, Amount.parseCurrency("100 USD"),
                Amount.parseCurrency("220 USD"), null, null, null, true, null,
                null, null);

        AuctionState output_lt_basePrice = new AuctionState(null, null, Amount.parseCurrency("100 USD"),
                Amount.parseCurrency("80 USD"), null, null, null, true, null,
                null, null);


        // Should fail auction is inactive
        transaction(ledgerServices, tx -> {
            tx.input(AuctionContract.ID, input_inactive);
            tx.output(AuctionContract.ID, output);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new AuctionContract.Commands.Bid());
            tx.fails();
            return null;
        });


        // Should fail bid price is less than base price
        transaction(ledgerServices, tx -> {
            tx.input(AuctionContract.ID, input);
            tx.output(AuctionContract.ID, output_lt_basePrice);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new AuctionContract.Commands.Bid());
            tx.fails();
            return null;
        });

        AuctionState output_lt_highestBid = new AuctionState(null, null, Amount.parseCurrency("100 USD"),
                Amount.parseCurrency("180 USD"), null, null, null, true, null,
                null, null);

        // Should fail bid price is less than previous highest bid
        transaction(ledgerServices, tx -> {
            tx.input(AuctionContract.ID, input);
            tx.output(AuctionContract.ID, output_lt_highestBid);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new AuctionContract.Commands.Bid());
            tx.fails();
            return null;
        });

        AuctionState output_eq_highestBid = new AuctionState(null, null, Amount.parseCurrency("100 USD"),
                Amount.parseCurrency("200 USD"), null, null, null, true, null,
                null, null);

        // Should fail bid price is equal to previous highest bid
        transaction(ledgerServices, tx -> {
            tx.input(AuctionContract.ID, input);
            tx.output(AuctionContract.ID, output_eq_highestBid);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new AuctionContract.Commands.Bid());
            tx.fails();
            return null;
        });

        //Should verify
        transaction(ledgerServices, tx -> {
            tx.input(AuctionContract.ID, input);
            tx.output(AuctionContract.ID, output);
            tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new AuctionContract.Commands.Bid());
            tx.verifies();
            return null;
        });
    }
}