package com.tutorial;

import com.google.common.collect.ImmutableList;
import com.tutorial.flows.CreateAndIssueAppleStamp;
import com.tutorial.flows.PackageApples;
import com.tutorial.flows.RedeemApples;
import com.tutorial.states.AppleStamp;
import com.tutorial.states.BasketOfApples;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RedeemApplesWithStampTest {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.tutorial.contracts"),
                TestCordapp.findCordapp("com.tutorial.flows")))
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
    public void buyerRedeemBasketOfApples() throws ExecutionException, InterruptedException {
        //Create Basket of Apples
        PackageApples.PackApplesInitiator createBasketOfApples = new PackageApples.PackApplesInitiator("Fuji4072", 10);
        Future<SignedTransaction> future = a.startFlow(createBasketOfApples);
        network.runNetwork();

        //Issue Apple Stamp
        CreateAndIssueAppleStamp.CreateAndIssueAppleStampInitiator issueAppleStamp =
                new CreateAndIssueAppleStamp.CreateAndIssueAppleStampInitiator(
                        "Fuji4072",this.b.getInfo().getLegalIdentities().get(0));
        Future<SignedTransaction> future1 = a.startFlow(issueAppleStamp);
        network.runNetwork();

        AppleStamp issuedStamp = (AppleStamp) future1.get().getTx().getOutputStates().get(0);
        UniqueIdentifier id = issuedStamp.getLinearId();

        //Redeem Basket of Apples with stamp
        RedeemApples.RedeemApplesInitiator redeemApples = new RedeemApples.RedeemApplesInitiator(b.getInfo().getLegalIdentities().get(0),id);
        Future<SignedTransaction> future2 = a.startFlow(redeemApples);
        network.runNetwork();

        //successful query means the state is stored at node b's vault. Flow went through.
        QueryCriteria outputCriteria = new QueryCriteria.VaultQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED);
        BasketOfApples state = b.getServices().getVaultService()
                .queryBy(BasketOfApples.class, outputCriteria).getStates().get(0).getState().getData();

        assert(state.getDescription().equals("Fuji4072"));
    }
}
