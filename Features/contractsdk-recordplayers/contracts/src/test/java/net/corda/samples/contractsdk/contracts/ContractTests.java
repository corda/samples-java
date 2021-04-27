package net.corda.samples.contractsdk.contracts;

import com.r3.corda.lib.contracts.contractsdk.StandardContract;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.samples.contractsdk.states.Needle;
import net.corda.samples.contractsdk.states.RecordPlayerState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();

    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands {
        }
    }

    private final Party alice = new TestIdentity(new CordaX500Name("Alice Audio", "", "GB")).getParty();
    private final Party bob = new TestIdentity(new CordaX500Name("Bob's Hustle Records", "", "GB")).getParty();

    @Test
    public void implementsContractTest() {
        assert (new RecordPlayerContract() instanceof Contract);
        assert (new RecordPlayerContract() instanceof StandardContract);
    }

    @Test
    public void mustIncludeValidCommand() {

        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        ledger(ledgerServices, l -> {

            // invalid command!
            l.transaction(tx -> {
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new Commands.DummyCommand());
                return tx.fails();
            });

            // valid create command
            l.transaction(tx -> {
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                return tx.verifies();
            });

            // valid update command
            l.transaction(tx -> {
                tx.input(RecordPlayerContract.ID, st);
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.verifies();
            });

            return null;
        });

    }


    @Test
    public void requiresCorrectNumberOfCommands() {

        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        ledger(ledgerServices, l -> {

            // 2+ issue commands
            l.transaction(tx -> {
                tx.input(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                return tx.fails();
            });

            // 2+ update commands
            l.transaction(tx -> {
                tx.input(RecordPlayerContract.ID, st);
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                return tx.fails();
            });

            return null;
        });

    }


    @Test
    public void requiresCorrectNumberOfInputsAndOutputs() {

        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        ledger(ledgerServices, l -> {

            // 2 inputs to issue, no outputs
            l.transaction(tx -> {
                tx.input(RecordPlayerContract.ID, st);
                tx.input(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                return tx.fails();
            });

            // 1 output to issue, no input
            l.transaction(tx -> {
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                return tx.verifies();
            });

            // 2 outputs to issue
            l.transaction(tx -> {
                tx.input(RecordPlayerContract.ID, st);
                tx.output(RecordPlayerContract.ID, st);
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                return tx.fails();
            });

            // 0 inputs to update
            l.transaction(tx -> {
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.fails();
            });

            // 2 inputs to update
            l.transaction(tx -> {
                tx.input(RecordPlayerContract.ID, st);
                tx.input(RecordPlayerContract.ID, st);
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.fails();
            });

            return null;
        });

    }


    @Test
    public void requireSpecificRoles() {

        RecordPlayerState st = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        ledger(ledgerServices, l -> {
            // must have manufacturer
            l.transaction(tx -> {
                tx.output(RecordPlayerContract.ID, st);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Issue());
                return tx.verifies();
            });

            return null;
        });

    }


    @Test
    public void additionalVerificationTests() {

        ledger(ledgerServices, l -> {

            // songs played decreases, should error
            l.transaction(tx -> {
                RecordPlayerState oldSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 20, new UniqueIdentifier());
                RecordPlayerState newSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 15, new UniqueIdentifier());

                tx.input(RecordPlayerContract.ID, oldSt);
                tx.output(RecordPlayerContract.ID, newSt);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.fails();
            });

            // songs played increases, valid
            l.transaction(tx -> {
                RecordPlayerState oldSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());
                RecordPlayerState newSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 800, 10000, 15, new UniqueIdentifier());

                tx.input(RecordPlayerContract.ID, oldSt);
                tx.output(RecordPlayerContract.ID, newSt);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.verifies();
            });

            // songs played the same, valid
            l.transaction(tx -> {
                RecordPlayerState oldSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());
                RecordPlayerState newSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 800, 10000, 0, new UniqueIdentifier());

                tx.input(RecordPlayerContract.ID, oldSt);
                tx.output(RecordPlayerContract.ID, newSt);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.verifies();
            });

            // negative coil turns on update, invalid
            l.transaction(tx -> {
                RecordPlayerState oldSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());
                RecordPlayerState newSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, -30, 10000, 15, new UniqueIdentifier());

                tx.input(RecordPlayerContract.ID, oldSt);
                tx.output(RecordPlayerContract.ID, newSt);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.fails();
            });

            // new magnet too strong, unsafe & invalid
            l.transaction(tx -> {
                RecordPlayerState oldSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());
                RecordPlayerState newSt = new RecordPlayerState(alice, bob, Needle.SPHERICAL, 9999999, 700, 10000, 15, new UniqueIdentifier());

                tx.input(RecordPlayerContract.ID, oldSt);
                tx.output(RecordPlayerContract.ID, newSt);
                tx.command(Arrays.asList(alice.getOwningKey(), bob.getOwningKey()), new RecordPlayerContract.Commands.Update());
                return tx.fails();
            });

            return null;
        });

    }


}
