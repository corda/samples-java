//package net.corda.samples.election.contracts;
//
//import net.corda.samples.election.states.VoteState;
//import net.corda.core.identity.CordaX500Name;
//import net.corda.testing.core.TestIdentity;
//import net.corda.testing.node.MockServices;
//import org.junit.Test;
//
//import java.util.Arrays;
//
//import static net.corda.testing.node.NodeTestUtils.ledger;
//
//
//public class ContractTests {
//    private final MockServices ledgerServices = new MockServices(Arrays.asList("net.corda.samples.election"));
//    TestIdentity alice = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
//    TestIdentity bob = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
//
//    @Test
//    public void issuerAndRecipientCannotHaveSameEmail() {
//        VoteState state = new VoteState("Hello-World",alice.getParty(),bob.getParty());
//        ledger(ledgerServices, l -> {
//            l.transaction(tx -> {
//                tx.input(VoteStateContract2.ID, state);
//                tx.output(VoteStateContract2.ID, state);
//                tx.command(alice.getPublicKey(), new VoteStateContract2.Commands.Send());
//                return tx.fails(); //fails because of having inputs
//            });
//            l.transaction(tx -> {
//                tx.output(VoteStateContract2.ID, state);
//                tx.command(alice.getPublicKey(), new VoteStateContract2.Commands.Send());
//                return tx.verifies();
//            });
//            return null;
//        });
//    }
//}