package net.corda.samples.chainmail.contracts;

import net.corda.samples.chainmail.states.ChainMailSessionState;
import junit.framework.TestCase;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Before;
import org.junit.Test;
import static net.corda.testing.node.NodeTestUtils.transaction;

import java.util.ArrayList;
import java.util.Arrays;

public class SantaSessionContractTests {

    private final TestIdentity santa = new TestIdentity(new CordaX500Name("Santa", "", "GB"));
    private final TestIdentity elf = new TestIdentity(new CordaX500Name("Santa", "", "GB"));

    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands{}
    }

    private final MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.chainmail.contracts")
    );

    private final ArrayList<String> playerNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"));
    private final ArrayList<String> playerEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"));

    private final ChainMailSessionState st = new ChainMailSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());

    private final ArrayList<String> noPlayerNames = new ArrayList<>();
    private final ArrayList<String> onePlayerNames = new ArrayList<>(Arrays.asList("david"));
    private final ArrayList<String> threePlayerNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "peter"));

    private final ArrayList<String> noPlayerEmails = new ArrayList<>();
    private final ArrayList<String> onePlayerEmails = new ArrayList<>(Arrays.asList("david@corda.net"));
    private final ArrayList<String> threePlayerEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "peter@corda.net"));


    @Before
    public void setup() { }

//    @After
//    public void tearDown() { }

    @Test
    public void SantaSessionContractImplementsContract() {
        assert(new ChainMailSessionContract() instanceof Contract);
    }

    @Test
    public void constructorTest() {
        ChainMailSessionState st = new ChainMailSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());
        TestCase.assertEquals(santa.getParty(), st.getIssuer());
        TestCase.assertEquals(playerNames, st.getPlayerNames());
        TestCase.assertEquals(playerEmails, st.getPlayerEmails());
    }

    @Test
    public void SantaSessionContractRequiresZeroInputsInTheTransaction() {
        ChainMailSessionState t1 = new ChainMailSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());
        ChainMailSessionState t2 = new ChainMailSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());

        transaction(ledgerServices, tx -> {
            // Has an input, will fail.
            tx.input(ChainMailSessionContract.ID, t1);
            tx.output(ChainMailSessionContract.ID, t2);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has no input, will verify.
            tx.output(ChainMailSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresOneOutputInTheTransaction() {
        transaction(ledgerServices, tx -> {
            // Has two outputs, will fail.
            tx.output(ChainMailSessionContract.ID, st);
            tx.output(ChainMailSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has one output, will verify.
            tx.output(ChainMailSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresOneCommandInTheTransaction() {
        transaction(ledgerServices, tx -> {
            tx.output(ChainMailSessionContract.ID, st);
            // Has two commands, will fail.
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(ChainMailSessionContract.ID, st);
            // Has one command, will verify.
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresTheTransactionsOutputToBeASantaSessionState() {
        transaction(ledgerServices, tx -> {
            // Has wrong output type, will fail.
            tx.output(ChainMailSessionContract.ID, new DummyState());
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct output type, will verify.
            tx.output(ChainMailSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        transaction(ledgerServices, tx -> {
            // Has wrong command type, will fail.
            tx.output(ChainMailSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey()), DummyCommandData.INSTANCE);
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct command type, will verify.
            tx.output(ChainMailSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresTheTransactionsOutputToHaveMoreThanThreePlayers() {

         ChainMailSessionState st = new ChainMailSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());
         ChainMailSessionState threePlayerSantaState = new ChainMailSessionState(threePlayerNames, threePlayerEmails, santa.getParty(), elf.getParty());

        transaction(ledgerServices, tx -> {
            // Has three players ChainMailSessionState, will verify.
            tx.output(ChainMailSessionContract.ID, threePlayerSantaState);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has over three players ChainMailSessionState, will verify.
            tx.output(ChainMailSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new ChainMailSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

}
