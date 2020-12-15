package net.corda.samples.stockpaydividend.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.stockpaydividend.states.StockState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    //sample tests
    @Test
    public void multipleOutputTests() {
        StockState tokenPass = new StockState(new UniqueIdentifier(),Operator.getParty(),"TT","Test Token",
                "USD",BigDecimal.valueOf(2.7), BigDecimal.valueOf(0.2),new Date(),new Date());
        StockState tokenFail = new StockState(new UniqueIdentifier(),Operator.getParty(),"","Test Token",
                "USD",BigDecimal.valueOf(2.7), BigDecimal.valueOf(0.2),new Date(),new Date());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(StockContract.CONTRACT_ID, tokenFail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); //Fail because multiple outputs
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(StockContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create());
                return tx.verifies();
            });
            return null;
        });
    }
}