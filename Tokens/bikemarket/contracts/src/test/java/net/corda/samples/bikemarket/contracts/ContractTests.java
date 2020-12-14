package net.corda.samples.bikemarket.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.bikemarket.states.FrameTokenState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    //sample tests
    @Test
    public void SerialNumberCannotBeEmpty() {
        FrameTokenState tokenPass = new FrameTokenState(Operator.getParty(),new UniqueIdentifier(),0,"8742");
        FrameTokenState tokenFail = new FrameTokenState(Operator.getParty(),new UniqueIdentifier(),0,"");
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(FrameContract.CONTRACT_ID, tokenFail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(FrameContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.verifies();
            });
            return null;
        });
    }
}