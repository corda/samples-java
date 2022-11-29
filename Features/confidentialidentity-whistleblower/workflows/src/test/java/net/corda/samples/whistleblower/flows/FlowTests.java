package net.corda.samples.whistleblower.flows;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import java.util.concurrent.ExecutionException;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters()
            .withCordappsForAllNodes(ImmutableList.of(
                    TestCordapp.findCordapp("net.corda.samples.whistleblower.contracts"),
                    TestCordapp.findCordapp("net.corda.samples.whistleblower.flows")))
            .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=Nakuru,C=KE")))));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode c = network.createNode();

    public FlowTests() {
        ImmutableList.of(a, b).forEach(it -> it.registerInitiatedFlow(BlowWhistleFlowResponder.class));
    }

    @Before
    public void setUp() throws Exception {
        network.startNodes();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    //simple unit test to check the public keys that used in the transaction
    //are different from both mock node a's legal public key and mock node b's legal public key.
    @Test
    public void dummyTest() throws ExecutionException, InterruptedException {
        CordaFuture<SignedTransaction> future = a.startFlow(new BlowWhistleFlow(b.getInfo().getLegalIdentities().get(0), c.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        SignedTransaction ptx = future.get();
        Assert.assertTrue(!ptx.getTx().getRequiredSigningKeys().contains(a.getInfo().getLegalIdentities().get(0).getOwningKey()));
        Assert.assertTrue(!ptx.getTx().getRequiredSigningKeys().contains(b.getInfo().getLegalIdentities().get(0).getOwningKey()));
    }
}
