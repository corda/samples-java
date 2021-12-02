package net.corda.samples.businessmembership;

import com.google.common.collect.ImmutableList;
import net.corda.bn.states.MembershipState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.samples.businessmembership.flows.membershipFlows.CreateNetwork;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.businessmembership.contracts"),
                TestCordapp.findCordapp("net.corda.samples.businessmembership.flows"),
                TestCordapp.findCordapp("net.corda.bn.flows"),
                TestCordapp.findCordapp("net.corda.bn.states")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
        );
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void createNetworkTest() throws ExecutionException, InterruptedException {
        CreateNetwork flow = new CreateNetwork();
        Future<String> future = a.startFlow(flow);
        network.runNetwork();
        String resString = future.get();
        System.out.println(resString);

        int subString = resString.indexOf("NetworkID: ");
        String networkId = resString.substring(subString+11);
        System.out.println("-"+ networkId+"-");

        QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED);
        MembershipState storedMembershipState = a.getServices().getVaultService()
                .queryBy(MembershipState.class,inputCriteria).getStates().get(0).getState().getData();
        System.out.println(storedMembershipState.getNetworkId());

        assert (storedMembershipState.getNetworkId().equals(networkId));

    }
}
