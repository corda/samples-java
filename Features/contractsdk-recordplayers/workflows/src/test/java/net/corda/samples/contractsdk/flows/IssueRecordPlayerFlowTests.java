package net.corda.samples.contractsdk.flows;


import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
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


/**
 * Practical exercise instructions Flows part 1.
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
public class IssueRecordPlayerFlowTests {

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
        // RecordPlayerState st = new RecordPlayerState(manufacturer, dealerB, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        IssueRecordPlayerFlow issueFlow = new IssueRecordPlayerFlow(dealerB, "SPHERICAL");
        CordaFuture<SignedTransaction> future = manufacturerNode.startFlow(issueFlow);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), signedTransaction.getNotary());
    }

    @Test
    public void contractCorrectness() throws Exception {
        RecordPlayerState st = new RecordPlayerState(manufacturer, dealerB, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        IssueRecordPlayerFlow issueFlow = new IssueRecordPlayerFlow(dealerB, "SPHERICAL");
        CordaFuture<SignedTransaction> future = manufacturerNode.startFlow(issueFlow);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();

        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals("net.corda.samples.contractsdk.contracts.RecordPlayerContract", output.getContract());
    }

    @Test
    public void canCreateState() throws Exception {
        RecordPlayerState st = new RecordPlayerState(manufacturer, dealerB, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());

        IssueRecordPlayerFlow issueFlow = new IssueRecordPlayerFlow(dealerB, "SPHERICAL");
        CordaFuture<SignedTransaction> future = manufacturerNode.startFlow(issueFlow);
        network.runNetwork();

        SignedTransaction signedTransaction = future.get();

        RecordPlayerState output = signedTransaction.getTx().outputsOfType(RecordPlayerState.class).get(0);

        // get some random data from the output to verify
        assertEquals(st.getManufacturer(), output.getManufacturer());
        assertEquals(st.getDealer(), output.getDealer());
        assertNotEquals(st.getDealer(), output.getManufacturer());
        assertEquals(st.getNeedle(), output.getNeedle());
    }

}
