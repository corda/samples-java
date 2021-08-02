package net.corda.samples.chainmail;

import com.google.common.collect.ImmutableList;
import net.corda.samples.chainmail.contracts.ChainMailSessionContract;
import net.corda.samples.chainmail.flows.CreateSantaSessionFlow;
import net.corda.samples.chainmail.states.ChainMailSessionState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionState;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CreateSantaSessionFlowTests {
    private MockNetwork network;

    private StartedMockNode santa;
    private StartedMockNode elf;

    private final NetworkParameters testNetworkParameters = new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    private final ArrayList<String> playerNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"));
    private final ArrayList<String> playerEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"));

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.chainmail.contracts"),
                TestCordapp.findCordapp("net.corda.samples.chainmail.flows"))).withNetworkParameters(testNetworkParameters)
        );

        santa = network.createPartyNode(null);
        elf = network.createPartyNode(null);

        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowUsesCorrectNotary() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();
        assertEquals(1, signedTransaction.getTx().getOutputStates().size());

        // ensure correct notary is used
        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), signedTransaction.getNotary());
    }

    @Test
    public void canCreateSession() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();
        assertEquals(1, signedTransaction.getTx().getOutputStates().size());

        ChainMailSessionState output = signedTransaction.getTx().outputsOfType(ChainMailSessionState.class).get(0);
        // get some random data from the output to verify
        assertEquals(playerNames, output.getPlayerNames());
    }

    @Test
    public void transactionConstructedHasCorrectOutput() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState tOutput = signedTransaction.getTx().getOutputs().get(0);

        // ensure correct notary is used is used
        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), tOutput.getNotary());

        ChainMailSessionState output = signedTransaction.getTx().outputsOfType(ChainMailSessionState.class).get(0);

        // checking player names, emails, and assignments.
        assertEquals(playerNames, output.getPlayerNames());
        assertEquals(playerEmails, output.getPlayerEmails());
        assertEquals(playerEmails.size(), output.getAssignments().size());
    }


    @Test
    public void transactionConstructedHasOneOutputUsingTheCorrectContract() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals("net.corda.samples.chainmail.contracts.ChainMailSessionContract", output.getContract());
    }


    @Test
    public void transactionConstructedByFlowHasOneIssueCommand() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assert (command.getValue() instanceof ChainMailSessionContract.Commands.Issue);
    }

    @Test
    public void transactionConstructedByFlowHasOneCommandWithTheIssuerAndTheOwnerAsASigners() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getCommands().size());
        Command command = signedTransaction.getTx().getCommands().get(0);

        assertEquals(2, command.getSigners().size());
        assertTrue(command.getSigners().contains(santa.getInfo().getLegalIdentities().get(0).getOwningKey()));
        assertTrue(command.getSigners().contains(elf.getInfo().getLegalIdentities().get(0).getOwningKey()));
    }

    @Test
    public void transactionConstructedByFlowHasNoInputsAttachmentsOrTimeWindows() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();
        assertEquals(0, signedTransaction.getTx().getInputs().size());
        assertEquals(1, signedTransaction.getTx().getOutputs().size());

        // The single attachment is the contract attachment.
        assertEquals(1, signedTransaction.getTx().getAttachments().size());
        assertNull(signedTransaction.getTx().getTimeWindow());
    }

}
