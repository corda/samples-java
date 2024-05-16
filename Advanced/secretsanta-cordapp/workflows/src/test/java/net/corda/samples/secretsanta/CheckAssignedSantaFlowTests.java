package net.corda.samples.secretsanta;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.secretsanta.flows.CheckAssignedSantaFlow;
import net.corda.samples.secretsanta.flows.CreateSantaSessionFlow;
import net.corda.samples.secretsanta.states.SantaSessionState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CheckAssignedSantaFlowTests {
    private MockNetwork network;
    private StartedMockNode santa;
    private StartedMockNode elf;

    private NetworkParameters testNetworkParameters = new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    private ArrayList<String> playerNames = new ArrayList<>(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"));
    private ArrayList<String> playerEmails = new ArrayList<>(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"));

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.secretsanta.contracts"),
                TestCordapp.findCordapp("net.corda.samples.secretsanta.flows"))).withNetworkParameters(testNetworkParameters)
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
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
        CheckAssignedSantaFlow f1 = new CheckAssignedSantaFlow(UniqueIdentifier.Companion.fromString("badUID"));
        CordaFuture<SantaSessionState> future1 = santa.startFlow(f1);
        network.runNetwork();

        SantaSessionState f1Output = future1.get();
    }

    @Test(expected = java.util.concurrent.ExecutionException.class)
    public void checkingMissingSessionID() throws Exception {
        CheckAssignedSantaFlow f1 = new CheckAssignedSantaFlow(UniqueIdentifier.Companion.fromString("8237fd23-3dab-4a9b-b2ea-671006a660b5"));
        CordaFuture<SantaSessionState> future1 = santa.startFlow(f1);
        network.runNetwork();

        SantaSessionState f1Output = future1.get();
    }

    // ensure we can create and query a santa session
    @Test
    public void flowProducesCorrectState() throws Exception {
        CreateSantaSessionFlow f1 = new CreateSantaSessionFlow(playerNames, playerEmails, elf.getInfo().getLegalIdentities().get(0));
        CordaFuture<SignedTransaction> future = santa.startFlow(f1);
        network.runNetwork();

        SantaSessionState f1Output = future.get().getTx().outputsOfType(SantaSessionState.class).get(0);
        CheckAssignedSantaFlow f2 = new CheckAssignedSantaFlow(f1Output.getLinearId());
        CordaFuture<SantaSessionState> future2 = santa.startFlow(f2);
        network.runNetwork();

        SantaSessionState f2Output = future2.get();

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

        SantaSessionState f1Output = future.get().getTx().outputsOfType(SantaSessionState.class).get(0);
        CheckAssignedSantaFlow f2 = new CheckAssignedSantaFlow(f1Output.getLinearId());
        CheckAssignedSantaFlow f3 = new CheckAssignedSantaFlow(f1Output.getLinearId());
        CordaFuture<SantaSessionState> future2 = santa.startFlow(f2);
        CordaFuture<SantaSessionState> future3 = elf.startFlow(f3);
        network.runNetwork();

        SantaSessionState f2Output = future2.get();
        SantaSessionState f3Output = future3.get();

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

        SantaSessionState f1Output = future.get().getTx().outputsOfType(SantaSessionState.class).get(0);

        String strSessionId = f1Output.getLinearId().toString();
        System.out.println("CONVERTED STRING ID: " + strSessionId);
        UniqueIdentifier convertedId = UniqueIdentifier.Companion.fromString(strSessionId);

        CheckAssignedSantaFlow f2 = new CheckAssignedSantaFlow(convertedId);
        CordaFuture<SantaSessionState> future2 = santa.startFlow(f2);
        network.runNetwork();

        SantaSessionState f2Output = future2.get();

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
