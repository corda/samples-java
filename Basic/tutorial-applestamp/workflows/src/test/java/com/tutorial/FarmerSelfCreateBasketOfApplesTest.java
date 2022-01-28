package com.tutorial;

import com.google.common.collect.ImmutableList;
import com.tutorial.flows.PackageApples;
import com.tutorial.states.BasketOfApples;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Future;

public class FarmerSelfCreateBasketOfApplesTest {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.tutorial.contracts"),
                TestCordapp.findCordapp("com.tutorial.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void createBasketOfApples(){
        PackageApples.PackApplesInitiator flow1 = new PackageApples.PackApplesInitiator("Fuji4072", 10);
        Future<SignedTransaction> future = a.startFlow(flow1);
        network.runNetwork();

        //successful query means the state is stored at node b's vault. Flow went through.
        QueryCriteria inputCriteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED);
        BasketOfApples state = a.getServices().getVaultService()
                .queryBy(BasketOfApples.class,inputCriteria).getStates().get(0).getState().getData();

        System.out.println("-------------------------");
        System.out.println(state.getOwner());
        System.out.println("-------------------------");

        assert(state.getDescription().equals("Fuji4072"));
    }
}

