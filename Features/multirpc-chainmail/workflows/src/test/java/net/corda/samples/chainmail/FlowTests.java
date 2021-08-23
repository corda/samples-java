package net.corda.samples.chainmail;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.NetworkParameters;
import net.corda.samples.chainmail.flows.SendMessage;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;
    private StartedMockNode d;

    private final NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(), 1, new LinkedHashMap<>());

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.chainmail.contracts"),
                TestCordapp.findCordapp("net.corda.samples.chainmail.flows"))).withNetworkParameters(testNetworkParameters));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);
        d = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

//    @Test
//    public void AccountCreation() throws ExecutionException, InterruptedException {
//        CreateNewAccount createAcct = new CreateNewAccount("alice-alice");
//        Future<String> future = a.startFlow(createAcct);
//        network.runNetwork();
//        AccountService accountService = a.getServices().cordaService(KeyManagementBackedAccountService.class);
//        // check that the account hash matches as the name
//        List<StateAndRef<AccountInfo>> myAccount = accountService.accountInfo("11714");
//        assert (myAccount.size() != 0);
//    }

    @Test
    public void SendMessageFlowTest() throws ExecutionException, InterruptedException {
        //create accounts
        Future<String> future = null;
        SendMessage message = new SendMessage("Hello, World!");
        future = a.startFlow(message);

    }
}