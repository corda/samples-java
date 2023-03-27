package net.corda.samples.tokentofriend.contracts;

import net.corda.samples.tokentofriend.states.CustomTokenState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    @Test
    public void issuerAndRecipientCannotHaveSameEmail() {
        CustomTokenState tokenPass = new CustomTokenState("Peter@gmail.com","David@gmail.com","Corda Number 1!", Operator.getParty(),0,new UniqueIdentifier());
        CustomTokenState tokenfail = new CustomTokenState("Peter@gmail.com","Peter@gmail.com","Corda Number 1!", Operator.getParty(),0,new UniqueIdentifier());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(CustomTokenContract.CONTRACT_ID, tokenfail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(CustomTokenContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.verifies();
            });
            return null;
        });
    }
    @Test
    public void recipientEmailCannotBeEmpty() {
        CustomTokenState tokenPass = new CustomTokenState("Peter@gmail.com","David@gmail.com","Corda Number 1!", Operator.getParty(),0,new UniqueIdentifier());
        CustomTokenState tokenfail = new CustomTokenState("Peter@gmail.com","","Corda Number 1!", Operator.getParty(),0,new UniqueIdentifier());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(CustomTokenContract.CONTRACT_ID, tokenfail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(CustomTokenContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create()); // Wrong type.
                return tx.verifies();
            });
            return null;
        });
    }
}