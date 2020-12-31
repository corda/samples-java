package net.corda.samples.tokenizedhouse;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.tokenizedhouse.flows.RealEstateEvolvableFungibleTokenFlow;
import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.tokenizedhouse.contracts"),
                TestCordapp.findCordapp("net.corda.samples.tokenizedhouse.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"))).withNetworkParameters(testNetworkParameters));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void houseTokenStateCreation() throws ExecutionException, InterruptedException {
        RealEstateEvolvableFungibleTokenFlow.CreateHouseTokenFlow createFlow =
                new RealEstateEvolvableFungibleTokenFlow.CreateHouseTokenFlow("NYCHelena",1000000);
        Future<SignedTransaction> future = a.startFlow(createFlow);
        network.runNetwork();

        //get house states on ledger with uuid as input tokenId
        StateAndRef<FungibleHouseTokenState> stateAndRef = a.getServices().getVaultService().
                queryBy(FungibleHouseTokenState.class).getStates().stream()
                .filter(sf->sf.getState().getData().getSymbol().equals("NYCHelena")).findAny()
                .orElseThrow(()-> new IllegalArgumentException("FungibleHouseTokenState not found from vault"));

        //get the RealEstateEvolvableTokenType object
        FungibleHouseTokenState tokenCreated = stateAndRef.getState().getData();
        assert (tokenCreated.getValuation() == 1000000);
    }

    @Test
    public void houseTokenStateIssuance() throws ExecutionException, InterruptedException {
        RealEstateEvolvableFungibleTokenFlow.CreateHouseTokenFlow createFlow =
                new RealEstateEvolvableFungibleTokenFlow.CreateHouseTokenFlow("NYCHelena",1000000);
        Future<SignedTransaction> future = a.startFlow(createFlow);
        network.runNetwork();

        RealEstateEvolvableFungibleTokenFlow.IssueHouseTokenFlow issueFlow =
                new RealEstateEvolvableFungibleTokenFlow.IssueHouseTokenFlow("NYCHelena",20,b.getInfo().getLegalIdentities().get(0));
        Future<SignedTransaction> future2 = a.startFlow(issueFlow);
        network.runNetwork();

        //get house states on ledger with uuid as input tokenId
        StateAndRef<FungibleHouseTokenState> stateAndRef = b.getServices().getVaultService().
                queryBy(FungibleHouseTokenState.class).getStates().stream()
                .filter(sf->sf.getState().getData().getSymbol().equals("NYCHelena")).findAny()
                .orElseThrow(()-> new IllegalArgumentException("FungibleHouseTokenState not found from vault"));

        //get the RealEstateEvolvableTokenType object
        FungibleHouseTokenState tokenCreated = stateAndRef.getState().getData();
        assert (tokenCreated.getValuation() == 1000000);
    }
}
