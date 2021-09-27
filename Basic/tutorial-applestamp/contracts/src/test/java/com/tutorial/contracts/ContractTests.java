package com.tutorial.contracts;

import com.tutorial.states.AppleStamp;
import com.tutorial.states.TemplateState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.ledger;


public class ContractTests {
    private final MockServices ledgerServices = new MockServices(Arrays.asList("com.tutorial.contracts"));
    TestIdentity alice = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
    TestIdentity bob = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    //Template Tester
    @Test
    public void issuerAndRecipientCannotHaveSameEmail() {
        TemplateState state = new TemplateState("Hello-World",alice.getParty(),bob.getParty());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(TemplateContract.ID, state);
                tx.output(TemplateContract.ID, state);
                tx.command(alice.getPublicKey(), new TemplateContract.Commands.Send());
                return tx.fails(); //fails because of having inputs
            });
            l.transaction(tx -> {
                tx.output(TemplateContract.ID, state);
                tx.command(alice.getPublicKey(), new TemplateContract.Commands.Send());
                return tx.verifies();
            });
            return null;
        });
    }

    //Basket of Apple cordapp testers
    @Test
    public void StampIssuanceCanOnlyHaveOneOutput(){
        AppleStamp stamp = new AppleStamp("FUji4072", alice.getParty(),bob.getParty(),new UniqueIdentifier());
        AppleStamp stamp2 = new AppleStamp("HoneyCrispy7864", alice.getParty(),bob.getParty(),new UniqueIdentifier());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(AppleStampContract.ID, stamp);
                tx.output(AppleStampContract.ID, stamp2);
                tx.command(alice.getPublicKey(), new AppleStampContract.Commands.Issue());
                return tx.fails(); //fails because of having inputs
            });
            l.transaction(tx -> {
                tx.output(AppleStampContract.ID, stamp);
                tx.command(alice.getPublicKey(), new AppleStampContract.Commands.Issue());
                return tx.verifies();
            });
            return null;
        });
    }

    @Test
    public void StampMustHaveDescription(){
        AppleStamp stamp = new AppleStamp("", alice.getParty(),bob.getParty(),new UniqueIdentifier());
        AppleStamp stamp2 = new AppleStamp("FUji4072", alice.getParty(),bob.getParty(),new UniqueIdentifier());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(AppleStampContract.ID, stamp);
                tx.command(Arrays.asList(alice.getPublicKey(),bob.getPublicKey()), new AppleStampContract.Commands.Issue());
                return tx.fails(); //fails because of having inputs
            });
            l.transaction(tx -> {
                tx.output(AppleStampContract.ID, stamp2);
                tx.command(Arrays.asList(alice.getPublicKey(),bob.getPublicKey()), new AppleStampContract.Commands.Issue());
                return tx.verifies();
            });
            return null;
        });
    }


}