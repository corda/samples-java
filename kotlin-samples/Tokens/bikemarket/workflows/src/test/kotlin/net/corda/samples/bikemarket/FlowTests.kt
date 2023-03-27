package net.corda.samples.bikemarket

import com.google.common.collect.ImmutableList
import net.corda.core.identity.CordaX500Name
import net.corda.samples.bikemarket.flows.CreateFrameToken
import net.corda.samples.bikemarket.flows.CreateWheelToken
import net.corda.samples.bikemarket.states.FrameTokenState
import net.corda.samples.bikemarket.states.WheelsTokenState
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Future

class FlowTests {
    private var network: MockNetwork? = null
    private var a: StartedMockNode? = null
    private var b: StartedMockNode? = null

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ).withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.bikemarket.contracts"),
                TestCordapp.findCordapp("net.corda.samples.bikemarket.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"))))
        a = network!!.createPartyNode(null)
        b = network!!.createPartyNode(null)
        network!!.runNetwork()
    }

    @After
    fun tearDown() {
        network!!.stopNodes()
    }

    @Test
    fun bikeTokensCreation() {
        val frameflow = CreateFrameToken("8742")
        val future: Future<String> = a!!.startFlow(frameflow)
        network!!.runNetwork()
        val wheelflow = CreateWheelToken("8755")
        val future2: Future<String> = a!!.startFlow(wheelflow)
        network!!.runNetwork()
        val (state) = a!!.services.vaultService.queryBy(FrameTokenState::class.java).states.stream().filter {
            (state) -> state.data.serialNum.equals("8742") }.findAny()
                .orElseThrow { IllegalArgumentException("frame serial symbol 8742 not found from vault") }
        val frameSerialStored: String = state.data.serialNum
        val (state1) = a!!.services.vaultService.queryBy(WheelsTokenState::class.java).states.stream().filter {
            (state1) -> state1.data.serialNum.equals("8755") }.findAny()
                .orElseThrow { IllegalArgumentException("wheel serial symbol 8755 not found from vault") }
        val wheelsSerialStored: String = state1.data.serialNum
        assert(frameSerialStored == "8742")
        assert(wheelsSerialStored == "8755")
    }
}