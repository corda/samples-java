package net.corda.samples.observable


import net.corda.core.identity.CordaX500Name
import net.corda.samples.observable.flows.TradeAndReport
import net.corda.samples.observable.states.HighlyRegulatedState
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("net.corda.samples.observable.contracts"),
            TestCordapp.findCordapp("net.corda.samples.observable.flows")),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
    ))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()
    private val d = network.createNode()

    @Throws
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun CheckIfObserverHaveTheStates() {
        a.startFlow(TradeAndReport(d.info.legalIdentities.get(0),
                b.info.legalIdentities[0], c.info.legalIdentities.get(0)))
        network.runNetwork()
        val bNodeStoredStates = b.services.vaultService.queryBy(HighlyRegulatedState::class.java)
                .states[0].state.data
        assert(bNodeStoredStates.participants.contains(d.info.legalIdentities.get(0)))
        val cNodeStoredStates: HighlyRegulatedState = c.services.vaultService.queryBy(HighlyRegulatedState::class.java)
                .states.get(0).state.data
        assert(cNodeStoredStates.participants.contains(d.info.legalIdentities.get(0)))
    }
}