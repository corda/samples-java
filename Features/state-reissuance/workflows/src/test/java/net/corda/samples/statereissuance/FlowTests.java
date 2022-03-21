package net.corda.samples.statereissuance;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.statereissuance.flows.IssueLandTitleFlow;
import net.corda.samples.statereissuance.flows.TransferLandTitleFlow;
import net.corda.samples.statereissuance.states.LandTitleState;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.statereissuance.contracts"),
                TestCordapp.findCordapp("net.corda.samples.statereissuance.flows")))
                .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB")))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void issueTest() throws Exception{
        LandTitleState landTitleState = issueLandTitle(a, b.getInfo().getLegalIdentities().get(0));
        assertNotNull(landTitleState);
        assertEquals(landTitleState.getIssuer(), a.getInfo().getLegalIdentities().get(0));
        assertEquals(landTitleState.getOwner(), b.getInfo().getLegalIdentities().get(0));
    }

    @Test
    public void transferTest() throws Exception{
        LandTitleState landTitleState = issueLandTitle(a, b.getInfo().getLegalIdentities().get(0));
        LandTitleState updatedLandTitleState = transferLandTitle(landTitleState, c.getInfo().getLegalIdentities().get(0), b);
        assertNotNull(updatedLandTitleState);
        assertEquals(updatedLandTitleState.getIssuer(), a.getInfo().getLegalIdentities().get(0));
        assertEquals(updatedLandTitleState.getOwner(), c.getInfo().getLegalIdentities().get(0));
    }

    private LandTitleState issueLandTitle(StartedMockNode issuer, Party owner) throws Exception{
        IssueLandTitleFlow.Initiator flow = new IssueLandTitleFlow.Initiator(
                owner, "60X40", "2000sqft"
        );
        CordaFuture<SignedTransaction> future = issuer.startFlow(flow);
        network.runNetwork();
        SignedTransaction transaction = future.get();
        LandTitleState landTitleState = (LandTitleState) transaction.getTx().getOutput(0);
        return landTitleState;
    }

    private LandTitleState transferLandTitle(LandTitleState landTitleState, Party newOwner, StartedMockNode owner) throws Exception {
        TransferLandTitleFlow.Initiator flow = new TransferLandTitleFlow.Initiator(
                landTitleState.getLinearId(), newOwner
        );
        CordaFuture<SignedTransaction> future = owner.startFlow(flow);
        network.runNetwork();
        SignedTransaction transaction = future.get();
        LandTitleState updatedLandTitleState = (LandTitleState) transaction.getTx().getOutput(0);
        return updatedLandTitleState;
    }

}
