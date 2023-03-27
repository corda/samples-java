package net.corda.samples.contractsdk.flows

import com.google.common.collect.ImmutableList
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NetworkParameters
import net.corda.core.utilities.getOrThrow
import net.corda.samples.contractsdk.states.Needle
import net.corda.samples.contractsdk.states.RecordPlayerState
import net.corda.testing.node.*
import org.junit.*
import java.time.Instant
import java.util.*
import kotlin.collections.LinkedHashMap

/**
 * Practical exercise instructions Flows part 1.
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
class IssueRecordPlayerFlowTests {

    lateinit var network: MockNetwork
    lateinit var manufacturerNode: StartedMockNode
    lateinit var dealerBNode: StartedMockNode
    lateinit var dealerCNode: StartedMockNode
    lateinit var manufacturer: Party
    lateinit var dealerB: Party
    lateinit var dealerC: Party

    val testNetworkParameters = NetworkParameters(4, Arrays.asList(), 10485760, 10485760 * 5, Instant.now(), 1, LinkedHashMap<String, List<SecureHash>>())

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ).withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("net.corda.samples.contractsdk.contracts"),
                TestCordapp.findCordapp("net.corda.samples.contractsdk.flows"))).withNetworkParameters(testNetworkParameters)
        )

        manufacturerNode = network.createPartyNode(null)
        dealerBNode = network.createPartyNode(null)
        dealerCNode = network.createPartyNode(null)
        manufacturer = manufacturerNode.info.legalIdentities[0]
        dealerB = dealerBNode.info.legalIdentities[0]
        dealerC = dealerCNode.info.legalIdentities[0]
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(Exception::class)
    fun flowUsesCorrectNotary() {
        // RecordPlayerState st = new RecordPlayerState(manufacturer, dealerB, Needle.SPHERICAL, 100, 700, 10000, 0, new UniqueIdentifier());
        val f1 = IssueRecordPlayerFlow(dealerB, "SPHERICAL")
        val future = manufacturerNode.startFlow(f1)
        network.runNetwork()
        val signedTransaction = future.get()

        if (signedTransaction != null) {
            Assert.assertEquals(1, signedTransaction.tx.outputStates.size)
        }
        Assert.assertEquals(network.notaryNodes[0].info.legalIdentities[0], signedTransaction?.notary)
    }

    @Test
    @Throws(Exception::class)
    fun contractCorrectness() {
        val issueFlow = IssueRecordPlayerFlow(dealerB, "SPHERICAL")
        val future = manufacturerNode.startFlow(issueFlow)
        network.runNetwork()

        val ptx = future.getOrThrow()

        val (_, contract) = ptx!!.tx.outputs.single()
        Assert.assertEquals("net.corda.samples.contractsdk.contracts.RecordPlayerContract", contract)
    }

    @Test
    @Throws(Exception::class)
    fun canCreateState() {
        val st = RecordPlayerState(manufacturer, dealerB, Needle.SPHERICAL, 100, 700, 10000, 0, UniqueIdentifier())
        val issueFlow = IssueRecordPlayerFlow(dealerB, "SPHERICAL")
        val future = manufacturerNode.startFlow(issueFlow)
        network.runNetwork()
        val signedTransaction = future.get()
        val output = signedTransaction!!.tx.outputsOfType(RecordPlayerState::class.java)[0]

        // get some random data from the output to verify
        Assert.assertEquals(st.manufacturer, output.manufacturer)
        Assert.assertEquals(st.dealer, output.dealer)
        Assert.assertNotEquals(st.dealer, output.manufacturer)
        Assert.assertEquals(st.needle, output.needle)
    }
}
