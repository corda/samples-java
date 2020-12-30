package net.corda.samples.supplychain.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.supplychain.states.InvoiceState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.UUID;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
    TestIdentity Operator2 = new TestIdentity(new CordaX500Name("Bob",  "TestLand",  "US"));

    @Test
    public void InvoiceAmountMustBeGreaterThanZero() {
        InvoiceState tokenPass = new InvoiceState(10, new AnonymousParty(Operator.getPublicKey()),new AnonymousParty(Operator2.getPublicKey()),new UniqueIdentifier().getId());
        InvoiceState tokenfail = new InvoiceState(-1, new AnonymousParty(Operator.getPublicKey()),new AnonymousParty(Operator2.getPublicKey()),new UniqueIdentifier().getId());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(InvoiceStateContract.ID, tokenfail);
                tx.command(Operator.getPublicKey(), new InvoiceStateContract.Commands.Create()); // Wrong type.
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(InvoiceStateContract.ID, tokenPass);
                tx.command(Operator.getPublicKey(), new InvoiceStateContract.Commands.Create()); // Wrong type.
                return tx.verifies();
            });
            return null;
        });
    }
}