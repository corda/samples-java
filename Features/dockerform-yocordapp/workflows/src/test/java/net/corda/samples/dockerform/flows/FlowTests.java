package net.corda.samples.dockerform.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
            TestCordapp.findCordapp("net.corda.samples.dockerform.contracts"),
            TestCordapp.findCordapp("net.corda.samples.dockerform.flows")
    )));
    private final StartedMockNode a = this.network.createNode();
    private final StartedMockNode b = this.network.createNode();

    public FlowTests() {
        ImmutableList.of(this.a, this.b).forEach(it -> {
            it.registerInitiatedFlow(YoFlowResponder.class);
        });
    }

    @Before
    public void setup() {
        this.network.runNetwork();
    }

    @After
    public void tearDown() {
        this.network.stopNodes();
    }

    //The yo flow should not have any input
    //This test will check if the input list is empty
    @Test
    public void dummyTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = this.a.startFlow(new YoFlow(this.b.getInfo().getLegalIdentities().get(0)));
        this.network.runNetwork();
        SignedTransaction ptx = future.get();
        assert (ptx.getTx().getInputs().isEmpty());
    }
}
