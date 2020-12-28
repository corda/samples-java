package net.corda.samples.autopayroll.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.autopayroll.states.MoneyState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.UUID;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity partya = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
    TestIdentity partyb = new TestIdentity(new CordaX500Name("Bob",  "TestLand",  "US"));

    @Test
    public void GameCanOnlyCreatedWhenTwoDifferentPlayerPresented() {
        MoneyState tokenPass = new MoneyState(10,partyb.getParty());
        MoneyState tokenfail = new MoneyState(-10,partyb.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(MoneyStateContract.ID, tokenfail);
                tx.command(partya.getPublicKey(), new MoneyStateContract.Commands.Pay()); // Wrong amount.
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(MoneyStateContract.ID, tokenPass);
                tx.command(partya.getPublicKey(), new MoneyStateContract.Commands.Pay());
                return tx.verifies();
            });
            return null;
        });
    }
}