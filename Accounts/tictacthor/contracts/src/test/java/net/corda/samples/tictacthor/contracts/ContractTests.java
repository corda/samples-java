package net.corda.samples.tictacthor.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.tictacthor.states.BoardState;
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
    public void GameCanOnlyCreatedWhenTwoDifferentPlayerPresented() {
        UniqueIdentifier playerX = new UniqueIdentifier();
        BoardState tokenPass = new BoardState(new UniqueIdentifier(),new UniqueIdentifier(),
                new AnonymousParty(Operator.getPublicKey()),new AnonymousParty(Operator2.getPublicKey()));
        BoardState tokenfail = new BoardState(playerX,playerX,
                new AnonymousParty(Operator.getPublicKey()),new AnonymousParty(Operator2.getPublicKey()));
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(BoardContract.ID, tokenfail);
                tx.command(Operator.getPublicKey(), new BoardContract.Commands.StartGame()); // Wrong type.
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(BoardContract.ID, tokenPass);
                tx.command(Operator.getPublicKey(), new BoardContract.Commands.StartGame()); // Wrong type.
                return tx.verifies();
            });
            return null;
        });
    }
}