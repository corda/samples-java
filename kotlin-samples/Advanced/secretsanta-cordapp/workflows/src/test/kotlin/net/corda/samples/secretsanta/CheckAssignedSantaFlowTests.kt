package net.corda.samples.secretsanta

import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NetworkParameters
import net.corda.samples.secretsanta.flows.CheckAssignedSantaFlow
import net.corda.samples.secretsanta.flows.CreateSantaSessionFlow
import net.corda.samples.secretsanta.states.SantaSessionState
import net.corda.testing.node.*
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException


class CheckAssignedSantaFlowTests {
    lateinit var network: MockNetwork
    lateinit var santa: StartedMockNode
    lateinit var elf: StartedMockNode

    private val testNetworkParameters = NetworkParameters(4, Arrays.asList(), 10485760, 10485760 * 5, Instant.now(), 1, java.util.LinkedHashMap<String, List<SecureHash>>())
    private val playerNames = ArrayList(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"))
    private val playerEmails = ArrayList(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"))


    @Before
    fun setup() {
        network = MockNetwork(
                MockNetworkParameters(cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("net.corda.samples.secretsanta.contracts"),
                        TestCordapp.findCordapp("net.corda.samples.secretsanta.flows")
                ),
                        networkParameters = testNetworkParameters,
                        notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
                ))
        santa = network.createNode(MockNodeParameters())
        elf = network.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(santa, elf)
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun checkingBadSessionID() {
        val f1 = CheckAssignedSantaFlow(fromString("badUID"))
        val future1 = santa.startFlow<SantaSessionState>(f1)
        network.runNetwork()
        val f1Output = future1.get()
    }

    @Test(expected = ExecutionException::class)
    @Throws(Exception::class)
    fun checkingMissingSessionID() {
        val f1 = CheckAssignedSantaFlow(fromString("8237fd23-3dab-4a9b-b2ea-671006a660b5"))
        val future1 = santa!!.startFlow<SantaSessionState>(f1)
        network!!.runNetwork()
        val f1Output = future1.get()
    }

    // ensure we can create and query a santa session
    @Test
    @Throws(Exception::class)
    fun flowProducesCorrectState() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow(f1)
        network!!.runNetwork()
        val f1Output = future.get().tx.outputsOfType(SantaSessionState::class.java)[0]
        val f2 = CheckAssignedSantaFlow(f1Output.linearId)
        val future2 = santa!!.startFlow<SantaSessionState>(f2)
        network!!.runNetwork()
        val f2Output = future2.get()
        assertEquals(playerNames, f1Output.playerNames)
        assertEquals(playerNames, f2Output.playerNames)

        // ensure these states are really the same
        assertEquals(f1Output.playerNames, f2Output.playerNames)
        assertEquals(f1Output.playerEmails, f2Output.playerEmails)
        assertEquals(f1Output.getAssignments(), f2Output.getAssignments())
        assert(f1Output.playerNames.contains("david"))
        assert(f1Output.playerNames.contains("olivia"))
        assert(!f1Output.playerNames.contains("derek"))
        assert(f2Output.playerNames.contains("david"))
        assert(f2Output.playerNames.contains("olivia"))
        assert(!f2Output.playerNames.contains("derek"))
        assertEquals(f1Output.getAssignments()!!["david"], f2Output.getAssignments()!!["david"])
        assertEquals(f1Output.getAssignments()!!["peter"], f2Output.getAssignments()!!["peter"])
        Assertions.assertNotEquals(f1Output.getAssignments()!!["peter"], f2Output.getAssignments()!!["david"])
        assertEquals(f1Output.linearId, f2Output.linearId)
    }

    @Test
    @Throws(Exception::class)
    fun bothNodesRetrieveTheSameState() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow(f1)
        network!!.runNetwork()
        val f1Output = future.get().tx.outputsOfType(SantaSessionState::class.java)[0]
        val f2 = CheckAssignedSantaFlow(f1Output.linearId)
        val f3 = CheckAssignedSantaFlow(f1Output.linearId)
        val future2 = santa!!.startFlow<SantaSessionState>(f2)
        val future3 = elf!!.startFlow<SantaSessionState>(f3)
        network!!.runNetwork()
        val f2Output = future2.get()
        val f3Output = future3.get()
        assertEquals(playerNames, f1Output.playerNames)
        assertEquals(playerNames, f2Output.playerNames)

        // ensure these states are really the same
        assertEquals(f2Output.playerNames, f3Output.playerNames)
        assertEquals(f2Output.playerEmails, f3Output.playerEmails)
        assertEquals(f2Output.getAssignments(), f3Output.getAssignments())
        assert(f2Output.playerNames.contains("david"))
        assert(f2Output.playerNames.contains("olivia"))
        assert(!f2Output.playerNames.contains("derek"))
        assert(f3Output.playerNames.contains("david"))
        assert(f3Output.playerNames.contains("olivia"))
        assert(!f3Output.playerNames.contains("derek"))
        assertEquals(f3Output.getAssignments()!!["david"], f2Output.getAssignments()!!["david"])
        assertEquals(f3Output.getAssignments()!!["peter"], f2Output.getAssignments()!!["peter"])
        Assertions.assertNotEquals(f3Output.getAssignments()!!["peter"], f2Output.getAssignments()!!["david"])
        assertEquals(f3Output.linearId, f2Output.linearId)
    }

    // ensure we can create and query a santa session
    @Test
    @Throws(Exception::class)
    fun canRetrieveWithConvertedStringId() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow(f1)
        network!!.runNetwork()
        val f1Output = future.get().tx.outputsOfType(SantaSessionState::class.java)[0]
        val strSessionId = f1Output.linearId.toString()
        println("CONVERTED STRING ID: $strSessionId")
        val convertedId = fromString(strSessionId)
        val f2 = CheckAssignedSantaFlow(convertedId)
        val future2 = santa!!.startFlow<SantaSessionState>(f2)
        network!!.runNetwork()
        val f2Output = future2.get()
        assertEquals(playerNames, f1Output.playerNames)
        assertEquals(playerNames, f2Output.playerNames)

        // ensure these states are really the same
        assertEquals(f1Output.playerNames, f2Output.playerNames)
        assertEquals(f1Output.playerEmails, f2Output.playerEmails)
        assertEquals(f1Output.getAssignments(), f2Output.getAssignments())
        assert(f1Output.playerNames.contains("david"))
        assert(f1Output.playerNames.contains("olivia"))
        assert(!f1Output.playerNames.contains("derek"))
        assert(f2Output.playerNames.contains("david"))
        assert(f2Output.playerNames.contains("olivia"))
        assert(!f2Output.playerNames.contains("derek"))
        assertEquals(f1Output.getAssignments()!!["david"], f2Output.getAssignments()!!["david"])
        assertEquals(f1Output.getAssignments()!!["peter"], f2Output.getAssignments()!!["peter"])
        Assertions.assertNotEquals(f1Output.getAssignments()!!["peter"], f2Output.getAssignments()!!["david"])
        assertEquals(f1Output.linearId, f2Output.linearId)
    }
}
