package net.corda.samples.dockerform.flows


import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ExecutionException


class FlowTests {

    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {

        val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.dockerform.contracts"),
                TestCordapp.findCordapp("net.corda.samples.dockerform.flows")
        ),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ))


        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(YoFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {

    }

    @Test
    fun `dummy test`() {
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    fun dummyTest() {
        val future = a.startFlow(YoFlow(b.info.legalIdentities.first()))
        mockNetwork.runNetwork()
        val ptx = future.get()
        if (ptx != null) {
            assert(ptx.tx.inputs.isEmpty())
        }
    }
}
