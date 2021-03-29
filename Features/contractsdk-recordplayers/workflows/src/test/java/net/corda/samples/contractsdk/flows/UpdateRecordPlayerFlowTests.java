package net.corda.samples.contractsdk.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.Party;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.contractsdk.states.Needle;
import net.corda.samples.contractsdk.states.RecordPlayerState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.jgroups.util.Util.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class UpdateRecordPlayerFlowTests {

    private MockNetwork network;
    private StartedMockNode manufacturerNode;
    private StartedMockNode dealerBNode;
    private StartedMockNode dealerCNode;

    private Party manufacturer;
    private Party dealerB;
    private Party dealerC;

    private NetworkParameters testNetworkParameters = new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(), 1, new LinkedHashMap<>());

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.contractsdk.contracts"),
                TestCordapp.findCordapp("net.corda.samples.contractsdk.flows"))).withNetworkParameters(testNetworkParameters)
        );

        manufacturerNode = network.createPartyNode(null);
        dealerBNode = network.createPartyNode(null);
        dealerCNode = network.createPartyNode(null);

        manufacturer = manufacturerNode.getInfo().getLegalIdentities().get(0);
        dealerB = dealerBNode.getInfo().getLegalIdentities().get(0);
        dealerC = dealerCNode.getInfo().getLegalIdentities().get(0);

        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void flowUsesCorrectNotary() throws Exception {

        IssueRecordPlayerFlow f1 = new IssueRecordPlayerFlow(dealerB, "SPHERICAL");
        CordaFuture<SignedTransaction> future = manufacturerNode.startFlow(f1);
        network.runNetwork();

        RecordPlayerState f1Output = future.get().getTx().outputsOfType(RecordPlayerState.class).get(0);

        UpdateRecordPlayerFlow f2 = new UpdateRecordPlayerFlow(f1Output.getLinearId(), "damaged", f1Output.getMagneticStrength(), f1Output.getCoilTurns(), f1Output.getAmplifierSNR(), f1Output.getSongsPlayed());
        CordaFuture<SignedTransaction> future2 = dealerBNode.startFlow(f2);
        network.runNetwork();

        RecordPlayerState f2Output = future2.get().getTx().outputsOfType(RecordPlayerState.class).get(0);

        SignedTransaction signedTransaction = future.get();

        // assert our contract SDK conditions
        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), signedTransaction.getNotary());
    }

    // ensure that our linear state updates work correctly
    @Test
    public void flowUpdateTest() throws Exception {
        IssueRecordPlayerFlow f1 = new IssueRecordPlayerFlow(dealerB, "SPHERICAL");
        CordaFuture<SignedTransaction> future = manufacturerNode.startFlow(f1);
        network.runNetwork();

        RecordPlayerState f1Output = future.get().getTx().outputsOfType(RecordPlayerState.class).get(0);

        UpdateRecordPlayerFlow f2 = new UpdateRecordPlayerFlow(
                f1Output.getLinearId(),
                "damaged",
                f1Output.getMagneticStrength(),
                f1Output.getCoilTurns(),
                f1Output.getAmplifierSNR(),
                f1Output.getSongsPlayed() + 5);

        CordaFuture<SignedTransaction> future2 = dealerBNode.startFlow(f2);
        network.runNetwork();

        RecordPlayerState f2Output = future2.get().getTx().outputsOfType(RecordPlayerState.class).get(0);

        assertEquals(Needle.SPHERICAL, f1Output.getNeedle());
        assertEquals(Needle.DAMAGED, f2Output.getNeedle());
        assertEquals(f1Output.getMagneticStrength(), f2Output.getMagneticStrength());
        assertEquals(f1Output.getSongsPlayed() + 5, f2Output.getSongsPlayed());
        assertNotEquals(f1Output.getSongsPlayed(), f2Output.getSongsPlayed());
    }


}

