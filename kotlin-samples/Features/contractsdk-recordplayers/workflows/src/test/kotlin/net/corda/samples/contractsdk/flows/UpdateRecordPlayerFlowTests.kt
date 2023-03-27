package net.corda.samples.contractsdk.flows

import com.google.common.collect.ImmutableList
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NetworkParameters
import net.corda.samples.contractsdk.states.Needle
import net.corda.samples.contractsdk.states.RecordPlayerState
import net.corda.testing.node.*
import org.junit.*
import java.time.Instant
import java.util.*
import kotlin.collections.LinkedHashMap

class UpdateRecordPlayerFlowTests {
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
        val f1 = IssueRecordPlayerFlow(dealerB, "SPHERICAL")
        val future = manufacturerNode.startFlow(f1)
        network.runNetwork()
        val f1Output = future.get()!!.tx.outputsOfType(RecordPlayerState::class.java)[0]
        val f2 = UpdateRecordPlayerFlow(f1Output.linearId, "damaged", f1Output.magneticStrength, f1Output.coilTurns, f1Output.amplifierSNR, f1Output.songsPlayed)
        val future2 = dealerBNode.startFlow(f2)
        network.runNetwork()
        val f2Output = future2.get()!!.tx.outputsOfType(RecordPlayerState::class.java)[0]
        val signedTransaction = future.get()

        // assert our contract SDK conditions
        Assert.assertEquals(1, signedTransaction!!.tx.outputStates.size)
        Assert.assertEquals(network.notaryNodes[0].info.legalIdentities[0], signedTransaction!!.notary)
    }

    // ensure that our linear state updates work correctly
    @Test
    @Throws(Exception::class)
    fun flowUpdateTest() {
        val f1 = IssueRecordPlayerFlow(dealerB, "SPHERICAL")
        val future = manufacturerNode.startFlow(f1)
        network!!.runNetwork()
        val f1Output = future.get()!!.tx.outputsOfType(RecordPlayerState::class.java)[0]

        val f2 = UpdateRecordPlayerFlow(
                f1Output.linearId,
                "damaged",
                f1Output.magneticStrength,
                f1Output.coilTurns,
                f1Output.amplifierSNR,
                f1Output.songsPlayed + 5)
        val future2 = dealerBNode.startFlow(f2)
        network.runNetwork()
        val f2Output = future2.get()!!.tx.outputsOfType(RecordPlayerState::class.java)[0]
        Assert.assertEquals(Needle.SPHERICAL, f1Output.needle)
        Assert.assertEquals(Needle.DAMAGED, f2Output.needle)
        Assert.assertEquals(f1Output.magneticStrength, f2Output.magneticStrength)
        Assert.assertEquals(f1Output.songsPlayed + 5, f2Output.songsPlayed)
        Assert.assertNotEquals(f1Output.songsPlayed.toLong(), f2Output.songsPlayed.toLong())
    }
}
