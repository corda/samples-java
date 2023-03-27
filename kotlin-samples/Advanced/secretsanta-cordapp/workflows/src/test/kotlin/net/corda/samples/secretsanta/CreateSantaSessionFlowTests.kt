package net.corda.samples.secretsanta


import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NetworkParameters
import net.corda.core.transactions.SignedTransaction
import net.corda.samples.secretsanta.contracts.SantaSessionContract
import net.corda.samples.secretsanta.flows.CreateSantaSessionFlow
import net.corda.samples.secretsanta.states.SantaSessionState
import net.corda.testing.node.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.time.Instant
import java.util.*


class CreateSantaSessionFlowTests {
    lateinit var network: MockNetwork
    lateinit var santa: StartedMockNode
    lateinit var elf: StartedMockNode

    private val testNetworkParameters = NetworkParameters(4, Arrays.asList(), 10485760, 10485760 * 5, Instant.now(), 1, LinkedHashMap<String, List<SecureHash>>())
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

    @Test
    @Throws(Exception::class)
    fun flowUsesCorrectNotary() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow<SignedTransaction>(f1)
        network!!.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        // ensure correct notary is used
        assertEquals(network!!.notaryNodes[0].info.legalIdentities[0], signedTransaction.notary)
    }

    @Test
    @Throws(Exception::class)
    fun canCreateSession() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow<SignedTransaction>(f1)
        network!!.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val output = signedTransaction.tx.outputsOfType(SantaSessionState::class.java)[0]
        // get some random data from the output to verify
        assertEquals(playerNames, output.playerNames)
    }

    @Test
    @Throws(Exception::class)
    fun transactionConstructedHasCorrectOutput() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow<SignedTransaction>(f1)
        network!!.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val (_, _, notary) = signedTransaction.tx.outputs[0]
        // ensure correct notary is used is used
        assertEquals(network!!.notaryNodes[0].info.legalIdentities[0], notary)
        val output = signedTransaction.tx.outputsOfType(SantaSessionState::class.java)[0]
        // checking player names, emails, and assignments.
        assertEquals(playerNames, output.playerNames)
        assertEquals(playerEmails, output.playerEmails)
        assertEquals(playerEmails.size, output.getAssignments()!!.size)
    }

    @Test
    @Throws(Exception::class)
    fun transactionConstructedHasOneOutputUsingTheCorrectContract() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow<SignedTransaction>(f1)
        network!!.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.outputStates.size)
        val (_, contract) = signedTransaction.tx.outputs[0]
        assertEquals("net.corda.samples.secretsanta.contracts.SantaSessionContract", contract)
    }

    @Test
    @Throws(Exception::class)
    fun transactionConstructedByFlowHasOneIssueCommand() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow<SignedTransaction>(f1)
        network!!.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.commands.size)
        val (value) = signedTransaction.tx.commands[0]
        assert(value is SantaSessionContract.Commands.Issue)
    }

    @Test
    @Throws(Exception::class)
    fun transactionConstructedByFlowHasOneCommandWithTheIssuerAndTheOwnerAsASigners() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow<SignedTransaction>(f1)
        network!!.runNetwork()
        val signedTransaction = future.get()
        assertEquals(1, signedTransaction.tx.commands.size)
        val (_, signers) = signedTransaction.tx.commands[0]
        assertEquals(2, signers.size)
        assertTrue(signers.contains(santa!!.info.legalIdentities[0].owningKey))
        assertTrue(signers.contains(elf!!.info.legalIdentities[0].owningKey))
    }

    @Test
    @Throws(Exception::class)
    fun transactionConstructedByFlowHasNoInputsAttachmentsOrTimeWindows() {
        val f1 = CreateSantaSessionFlow(playerNames, playerEmails, elf!!.info.legalIdentities[0])
        val future = santa!!.startFlow<SignedTransaction>(f1)
        network!!.runNetwork()
        val signedTransaction = future.get()
        assertEquals(0, signedTransaction.tx.inputs.size)
        assertEquals(1, signedTransaction.tx.outputs.size)
        // The single attachment is the contract attachment.
        assertEquals(1, signedTransaction.tx.attachments.size)
        Assertions.assertNull(signedTransaction.tx.timeWindow)
    }
}
