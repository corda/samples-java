package net.corda.samples.chainmail;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.NetworkParameters;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.chainmail.flows.SendMessage;
import net.corda.samples.chainmail.states.MessageState;
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
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    private final NetworkParameters testNetworkParameters =
            new NetworkParameters(4, Arrays.asList(), 10485760, (10485760 * 5), Instant.now(),1, new LinkedHashMap<>());

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.chainmail.contracts"),
                TestCordapp.findCordapp("net.corda.samples.chainmail.flows"))).withNetworkParameters(testNetworkParameters));
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
    public void MessageFlowTest() throws ExecutionException, InterruptedException {
        String messageText = "Hello, World!";
        SendMessage message = new SendMessage(messageText);
        CordaFuture<SignedTransaction> future1 = a.startFlow(message);
        network.runNetwork();

        for (StartedMockNode node : ImmutableList.of(a, b, c)) {
            node.transaction(() -> {
                List<StateAndRef<MessageState>> messages = node.getServices().getVaultService().queryBy(MessageState.class).getStates();
                assertEquals(1, messages.size());
                MessageState recordedState = messages.get(0).getState().getData();
                assertEquals(recordedState.getMessage(), messageText);
                return null;
            });
        }
    }
}
