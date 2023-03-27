package net.corda.samples.whistleblower.flows

import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("net.corda.samples.whistleblower.contracts"),
            TestCordapp.findCordapp("net.corda.samples.whistleblower.flows")
    ),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","Nakuru","KE")))
    ))
    private val a = network.createNode()
    private val b = network.createNode()
    private val c = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(BlowWhistleFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    //simple unit test to check the public keys that used in the transaction
    //are different from both mock node a's legal public key and mock node b's legal public key.
    @Test
    fun `dummy test`() {
        val future = a.startFlow(BlowWhistleFlow(b.info.legalIdentities.first(), c.info.legalIdentities.first()))
        network.runNetwork()
        val ptx = future.get()
        assert(!ptx.tx.requiredSigningKeys.contains(a.info.legalIdentities.first().owningKey))
        assert(!ptx.tx.requiredSigningKeys.contains(b.info.legalIdentities.first().owningKey))

    }
}
