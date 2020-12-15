package net.corda.samples.bikemarket;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.samples.bikemarket.flows.CreateFrameToken;
import net.corda.samples.bikemarket.flows.CreateWheelToken;
import net.corda.samples.bikemarket.states.FrameTokenState;
import net.corda.samples.bikemarket.states.WheelsTokenState;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.corda.testing.node.StartedMockNode;
import java.util.concurrent.Future;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;

    @Before
    public void setup() {

        network = new MockNetwork(new MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.bikemarket.contracts"),
                TestCordapp.findCordapp("net.corda.samples.bikemarket.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"))));
        a = network.createPartyNode(null);
        b = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void bikeTokensCreation() {
        CreateFrameToken frameflow = new CreateFrameToken("8742");
        Future<String> future = a.startFlow(frameflow);
        network.runNetwork();
        CreateWheelToken wheelflow = new CreateWheelToken("8755");
        Future<String> future2 = a.startFlow(wheelflow);
        network.runNetwork();
        StateAndRef<FrameTokenState> frameStateStateAndRef = a.getServices().getVaultService().
                queryBy(FrameTokenState.class).getStates().stream().filter(sf->sf.getState().getData().getserialNum().equals("8742")).findAny()
                .orElseThrow(()-> new IllegalArgumentException("frame serial symbol 8742 not found from vault"));
        String frameSerialStored = frameStateStateAndRef.getState().getData().getserialNum();
        StateAndRef<WheelsTokenState> wheelStateStateAndRef = a.getServices().getVaultService().
                queryBy(WheelsTokenState.class).getStates().stream().filter(sf->sf.getState().getData().getserialNum().equals("8755")).findAny()
                .orElseThrow(()-> new IllegalArgumentException("wheel serial symbol 8755 not found from vault"));
        String wheelsSerialStored = wheelStateStateAndRef.getState().getData().getserialNum();
        assert (frameSerialStored.equals("8742"));
        assert (wheelsSerialStored.equals("8755"));
    }
}
