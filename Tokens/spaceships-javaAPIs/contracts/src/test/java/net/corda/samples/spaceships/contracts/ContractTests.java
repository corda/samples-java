package net.corda.samples.spaceships.contracts;

import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.spaceships.states.SpaceshipTokenType;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    @Test
    public void issuerAndRecipientCannotHaveSameEmail() {
        SpaceshipTokenType tokenPass = new SpaceshipTokenType(
                Operator.getParty(),"CordaNumber1","Earth", 9, AmountUtilities.amount(1000, FiatCurrency.getInstance("USD")) ,true);
        SpaceshipTokenType tokenFail = new SpaceshipTokenType(
                Operator.getParty(),"CordaNumber1","", 9, AmountUtilities.amount(1000, FiatCurrency.getInstance("USD")) ,true);

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(SpaceshipTokenContract.CONTRACT_ID, tokenFail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(SpaceshipTokenContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.verifies();
            });
            return null;
        });
    }
}