package net.corda.samples.observable.contracts;

import net.corda.core.identity.CordaX500Name;
import net.corda.samples.observable.states.HighlyRegulatedState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity partya = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
    TestIdentity partyb = new TestIdentity(new CordaX500Name("Bob",  "TestLand",  "US"));

    @Test
    public void TheBuyerandthesellercannotbethesame() {
        HighlyRegulatedState tokenPass = new HighlyRegulatedState(partya.getParty(),partyb.getParty());
        HighlyRegulatedState tokenfail = new HighlyRegulatedState(partyb.getParty(),partyb.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(HighlyRegulatedContract.ID, tokenfail);
                tx.command(Arrays.asList(partya.getPublicKey(),partyb.getPublicKey()), new HighlyRegulatedContract.Commands.Trade()); // Same buyer & seller
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(HighlyRegulatedContract.ID, tokenPass);
                tx.command(Arrays.asList(partya.getPublicKey(),partyb.getPublicKey()), new HighlyRegulatedContract.Commands.Trade());
                return tx.verifies();
            });
            return null;
        });
    }
}