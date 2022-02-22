package net.corda.samples.avatar;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.avatar.flows.CreateAvatarFlow;
import net.corda.samples.avatar.flows.TransferAvatarFlow;
import net.corda.samples.avatar.states.Avatar;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.avatar.contracts"),
                TestCordapp.findCordapp("net.corda.samples.avatar.flows"))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void CreateAvatarTest() {
        CreateAvatarFlow createflow = new CreateAvatarFlow("PETER-7526", 3);
        CordaFuture<SignedTransaction> future = a.startFlow(createflow);
        network.runNetwork();

        //successful query means the state is stored at node b's vault. Flow went through.
        QueryCriteria.VaultQueryCriteria inputCriteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED);
        Avatar state = a.getServices().getVaultService().queryBy(Avatar.class,inputCriteria).getStates().get(0).getState().getData();
    }
}
