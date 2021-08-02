package net.corda.samples.chainmail;

import com.google.common.collect.ImmutableList;
import net.corda.samples.chainmail.flows.CheckAssignedChainMailFlow;
import net.corda.samples.chainmail.flows.CreateSantaSessionFlow;
import net.corda.samples.chainmail.states.ChainMailSessionState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
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
import java.util.*;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CheckAssignedSantaFlowTests {
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

    @Test(expected = IllegalArgumentException.class)
    public void checkingBadSessionID() throws Exception {
        CheckAssignedChainMailFlow f1 = new CheckAssignedChainMailFlow(UniqueIdentifier.Companion.fromString("badUID"));
        CordaFuture<ChainMailSessionState> future1 = santa.startFlow(f1);
        network.runNetwork();

        ChainMailSessionState f1Output = future1.get();
    }

    @Test(expected = java.util.concurrent.ExecutionException.class)
    public void checkingMissingSessionID() throws Exception {
        CheckAssignedChainMailFlow f1 = new CheckAssignedChainMailFlow(UniqueIdentifier.Companion.fromString("8237fd23-3dab-4a9b-b2ea-671006a660b5"));
        CordaFuture<ChainMailSessionState> future1 = santa.startFlow(f1);
        network.runNetwork();

        ChainMailSessionState f1Output = future1.get();
    }

    // ensure we can create and query a santa session
    @Test
    public void flowProducesCorrectState() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        ChainMailSessionState f1Output = future.get().getTx().outputsOfType(ChainMailSessionState.class).get(0);
        CheckAssignedChainMailFlow f2 = new CheckAssignedChainMailFlow(f1Output.getLinearId());
        CordaFuture<ChainMailSessionState> future2 = santa.startFlow(f2);
        network.runNetwork();

        ChainMailSessionState f2Output = future2.get();

        assertEquals(playerNames, f1Output.getPlayerNames());
        assertEquals(playerNames, f2Output.getPlayerNames());

        // ensure these states are really the same
        assertEquals(f1Output.getPlayerNames(), f2Output.getPlayerNames());
        assertEquals(f1Output.getPlayerEmails(), f2Output.getPlayerEmails());
        assertEquals(f1Output.getAssignments(), f2Output.getAssignments());

        assert(f1Output.getPlayerNames().contains("david"));
        assert(f1Output.getPlayerNames().contains("olivia"));
        assert(!f1Output.getPlayerNames().contains("derek"));

        assert(f2Output.getPlayerNames().contains("david"));
        assert(f2Output.getPlayerNames().contains("olivia"));
        assert(!f2Output.getPlayerNames().contains("derek"));

        assertEquals(f1Output.getAssignments().get("david"), f2Output.getAssignments().get("david"));
        assertEquals(f1Output.getAssignments().get("peter"), f2Output.getAssignments().get("peter"));
        assertNotEquals(f1Output.getAssignments().get("peter"), f2Output.getAssignments().get("david"));

        assertEquals(f1Output.getLinearId(), f2Output.getLinearId());
    }


    @Test
    public void bothNodesRetrieveTheSameState() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        ChainMailSessionState f1Output = future.get().getTx().outputsOfType(ChainMailSessionState.class).get(0);
        CheckAssignedChainMailFlow f2 = new CheckAssignedChainMailFlow(f1Output.getLinearId());
        CheckAssignedChainMailFlow f3 = new CheckAssignedChainMailFlow(f1Output.getLinearId());
        CordaFuture<ChainMailSessionState> future2 = santa.startFlow(f2);
        CordaFuture<ChainMailSessionState> future3 = elf.startFlow(f3);
        network.runNetwork();

        ChainMailSessionState f2Output = future2.get();
        ChainMailSessionState f3Output = future3.get();

        assertEquals(playerNames, f1Output.getPlayerNames());
        assertEquals(playerNames, f2Output.getPlayerNames());

        // ensure these states are really the same
        assertEquals(f2Output.getPlayerNames(), f3Output.getPlayerNames());
        assertEquals(f2Output.getPlayerEmails(), f3Output.getPlayerEmails());
        assertEquals(f2Output.getAssignments(), f3Output.getAssignments());

        assert(f2Output.getPlayerNames().contains("david"));
        assert(f2Output.getPlayerNames().contains("olivia"));
        assert(!f2Output.getPlayerNames().contains("derek"));

        assert(f3Output.getPlayerNames().contains("david"));
        assert(f3Output.getPlayerNames().contains("olivia"));
        assert(!f3Output.getPlayerNames().contains("derek"));

        assertEquals(f3Output.getAssignments().get("david"), f2Output.getAssignments().get("david"));
        assertEquals(f3Output.getAssignments().get("peter"), f2Output.getAssignments().get("peter"));
        assertNotEquals(f3Output.getAssignments().get("peter"), f2Output.getAssignments().get("david"));

        assertEquals(f3Output.getLinearId(), f2Output.getLinearId());
    }

    // ensure we can create and query a santa session
    @Test
    public void canRetrieveWithConvertedStringId() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        ChainMailSessionState f1Output = future.get().getTx().outputsOfType(ChainMailSessionState.class).get(0);

        String strSessionId = f1Output.getLinearId().toString();
        System.out.println("CONVERTED STRING ID: " + strSessionId);
        UniqueIdentifier convertedId = UniqueIdentifier.Companion.fromString(strSessionId);

        CheckAssignedChainMailFlow f2 = new CheckAssignedChainMailFlow(convertedId);
        CordaFuture<ChainMailSessionState> future2 = santa.startFlow(f2);
        network.runNetwork();

        ChainMailSessionState f2Output = future2.get();

        assertEquals(playerNames, f1Output.getPlayerNames());
        assertEquals(playerNames, f2Output.getPlayerNames());

        // ensure these states are really the same
        assertEquals(f1Output.getPlayerNames(), f2Output.getPlayerNames());
        assertEquals(f1Output.getPlayerEmails(), f2Output.getPlayerEmails());
        assertEquals(f1Output.getAssignments(), f2Output.getAssignments());

        assert(f1Output.getPlayerNames().contains("david"));
        assert(f1Output.getPlayerNames().contains("olivia"));
        assert(!f1Output.getPlayerNames().contains("derek"));

        assert(f2Output.getPlayerNames().contains("david"));
        assert(f2Output.getPlayerNames().contains("olivia"));
        assert(!f2Output.getPlayerNames().contains("derek"));

        assertEquals(f1Output.getAssignments().get("david"), f2Output.getAssignments().get("david"));
        assertEquals(f1Output.getAssignments().get("peter"), f2Output.getAssignments().get("peter"));
        assertNotEquals(f1Output.getAssignments().get("peter"), f2Output.getAssignments().get("david"));

        assertEquals(f1Output.getLinearId(), f2Output.getLinearId());
    }

}
