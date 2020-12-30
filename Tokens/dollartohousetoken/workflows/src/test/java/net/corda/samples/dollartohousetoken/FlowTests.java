package net.corda.samples.dollartohousetoken;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.NetworkParameters;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.samples.dollartohousetoken.flows.HouseTokenCreateAndIssueFlow;
import net.corda.samples.dollartohousetoken.states.HouseState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.corda.testing.node.StartedMockNode;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.dollartohousetoken.contracts"),
                TestCordapp.findCordapp("net.corda.samples.dollartohousetoken.flows"),
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
        HouseTokenCreateAndIssueFlow createAndIssueFlow = new HouseTokenCreateAndIssueFlow(b.getInfo().getLegalIdentities().get(0),
                Amount.parseCurrency("1000 USD"),10,
                "500 sqft","NA","NYC");
        Future<String> future = a.startFlow(createAndIssueFlow);
        network.runNetwork();
        String resultString = future.get();
        System.out.println("\n"+ resultString+"");
        int subString = resultString.indexOf("UUID: ");
        String nonfungibleTokenId = resultString.substring(subString+6,resultString.indexOf(". (This"));
        System.out.println("-"+ nonfungibleTokenId+"-");
        QueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria().withUuid(Arrays.asList(UUID.fromString(nonfungibleTokenId))).withStatus(Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<HouseState>> storedNonFungibleTokenb = b.getServices().getVaultService().queryBy(HouseState.class,inputCriteria).getStates();
        HouseState storedToken = storedNonFungibleTokenb.get(0).getState().getData();
        System.out.println("-"+ storedToken.getLinearId().toString()+"-");
        assert (storedToken.getLinearId().toString().equals(nonfungibleTokenId));
    }
}