package net.corda.examples.notarychange;

import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters()
        .withCordappsForAllNodes(
                ImmutableList.of(
                TestCordapp.findCordapp("net.corda.examples.autopayroll.contracts"),
                TestCordapp.findCordapp("net.corda.examples.autopayroll.flows")
                )
        )
    );

    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode bank = network.createNode(new CordaX500Name("PartyA", "Toronto", "CA"));


    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

}
