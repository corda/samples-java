package net.corda.samples.heartbeat.flows

import net.corda.client.rpc.notUsed
import net.corda.core.identity.CordaX500Name
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var node: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(
                MockNetworkParameters(threadPerNode = true, cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("net.corda.samples.heartbeat.flows"),
                        TestCordapp.findCordapp("net.corda.samples.heartbeat.contracts")),
                        networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                        notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
                )

        )
        node = network.createNode()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `heartbeat occurs every second`() {
        val flow = StartHeartbeatFlow()
        node.startFlow(flow).get()

        val sleepTime: Long = 6000
        Thread.sleep(sleepTime)

        val recordedTxs = node.transaction {
            val (recordedTxs, futureTxs) = node.services.validatedTransactions.track()
            futureTxs.notUsed()
            recordedTxs
        }

        val totalExpectedTransactions = 7
        assertEquals(totalExpectedTransactions, recordedTxs.size)
    }
}
