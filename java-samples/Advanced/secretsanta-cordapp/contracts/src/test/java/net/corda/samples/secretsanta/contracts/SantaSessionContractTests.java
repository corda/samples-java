package net.corda.samples.secretsanta.contracts;

import net.corda.samples.secretsanta.states.SantaSessionState;
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

    private MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.secretsanta.contracts")
    );

    private ArrayList<String> playerNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"));
    private ArrayList<String> playerEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"));

    private SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());

    private ArrayList<String> noPlayerNames = new ArrayList<>();
    private ArrayList<String> onePlayerNames = new ArrayList<>(Arrays.asList("david"));
    private ArrayList<String> threePlayerNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "peter"));

    private ArrayList<String> noPlayerEmails = new ArrayList<>();
    private ArrayList<String> onePlayerEmails = new ArrayList<>(Arrays.asList("david@corda.net"));
    private ArrayList<String> threePlayerEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "peter@corda.net"));


    @Before
    public void setup() { }

//    @After
//    public void tearDown() { }

    @Test
    public void SantaSessionContractImplementsContract() {
        assert(new SantaSessionContract() instanceof Contract);
    }

    @Test
    public void constructorTest() {
        SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());
        TestCase.assertEquals(santa.getParty(), st.getIssuer());
        TestCase.assertEquals(playerNames, st.getPlayerNames());
        TestCase.assertEquals(playerEmails, st.getPlayerEmails());
    }

    @Test
    public void SantaSessionContractRequiresZeroInputsInTheTransaction() {
        SantaSessionState t1 = new SantaSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());
        SantaSessionState t2 = new SantaSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());

        transaction(ledgerServices, tx -> {
            // Has an input, will fail.
            tx.input(SantaSessionContract.ID, t1);
            tx.output(SantaSessionContract.ID, t2);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has no input, will verify.
            tx.output(SantaSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresOneOutputInTheTransaction() {
        transaction(ledgerServices, tx -> {
            // Has two outputs, will fail.
            tx.output(SantaSessionContract.ID, st);
            tx.output(SantaSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has one output, will verify.
            tx.output(SantaSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresOneCommandInTheTransaction() {
        transaction(ledgerServices, tx -> {
            tx.output(SantaSessionContract.ID, st);
            // Has two commands, will fail.
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(SantaSessionContract.ID, st);
            // Has one command, will verify.
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresTheTransactionsOutputToBeASantaSessionState() {
        transaction(ledgerServices, tx -> {
            // Has wrong output type, will fail.
            tx.output(SantaSessionContract.ID, new DummyState());
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct output type, will verify.
            tx.output(SantaSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        transaction(ledgerServices, tx -> {
            // Has wrong command type, will fail.
            tx.output(SantaSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey()), DummyCommandData.INSTANCE);
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct command type, will verify.
            tx.output(SantaSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void SantaSessionContractRequiresTheTransactionsOutputToHaveMoreThanThreePlayers() {

         SantaSessionState st = new SantaSessionState(playerNames, playerEmails, santa.getParty(), elf.getParty());
         SantaSessionState threePlayerSantaState = new SantaSessionState(threePlayerNames, threePlayerEmails, santa.getParty(), elf.getParty());

        transaction(ledgerServices, tx -> {
            // Has three players SantaSessionState, will verify.
            tx.output(SantaSessionContract.ID, threePlayerSantaState);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has over three players SantaSessionState, will verify.
            tx.output(SantaSessionContract.ID, st);
            tx.command(Arrays.asList(santa.getPublicKey(), elf.getPublicKey()), new SantaSessionContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

}
