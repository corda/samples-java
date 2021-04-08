package net.corda.samples.businessmembership.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.businessmembership.states.InsuranceState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.businessmembership.contracts"));
    TestIdentity alice = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
    TestIdentity bob = new TestIdentity(new CordaX500Name("Bob",  "TestLand",  "US"));

    @Test
    public void issuerAndRecipientCannotHaveSameEmail() {
        InsuranceState insurancestate = new InsuranceState(alice.getParty(), "TEST", bob.getParty(), new UniqueIdentifier().toString(), "Initiating Policy");
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(InsuranceStateContract.InsuranceStateContract_ID, insurancestate);
                tx.command(alice.getPublicKey(), new InsuranceStateContract.Commands.Issue()); // Wrong type.
                return tx.fails();
            });
            return null;
        });
    }
}