package net.corda.samples.dollartohousetoken.contracts;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.dollartohousetoken.states.HouseState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    //sample tests
    @Test
    public void valuationCannotBeZero() {
        HouseState tokenPass = new HouseState(new UniqueIdentifier(),
                Arrays.asList(Operator.getParty()),
                Amount.parseCurrency("1000 USD"),
                10,"500sqft",
                "none","NYC");
        HouseState tokenFail = new HouseState(new UniqueIdentifier(),
                Arrays.asList(Operator.getParty()),
                Amount.parseCurrency("0 USD"),
                10,"500sqft",
                "none","NYC");
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(HouseContract.CONTRACT_ID, tokenFail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create());
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(HouseContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create());
                return tx.verifies();
            });
            return null;
        });
    }
}