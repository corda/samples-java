package net.corda.samples.obligation.flow

import net.corda.samples.obligation.flows.IOUIssueFlowResponder
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.*
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.*
import net.corda.samples.obligation.contract.IOUContract
import net.corda.samples.obligation.flows.IOUIssueFlow
import net.corda.samples.obligation.states.IOUState
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Practical exercise instructions Flows part 1.
 * Uncomment the unit tests and use the hints + unit test body to complete the Flows such that the unit tests pass.
 * Note! These tests rely on Quasar to be loaded, set your run configuration to "-ea -javaagent:lib/quasar.jar"
 * Run configuration can be edited in IntelliJ under Run -> Edit Configurations -> VM options
 * On some machines/configurations you may have to provide a full path to the quasar.jar file.
 * On some machines/configurations you may have to use the "JAR manifest" option for shortening the command line.
 */
class IOUIssueFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(
            listOf("net.corda.samples.obligation"),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        )
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(IOUIssueFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    /**
     * Task 1.
     * Build out the [IOUIssueFlow]!
     * TODO: Implement the [IOUIssueFlow] flow which builds and returns a partially [SignedTransaction].
     * Hint:
     * - There's a lot to do to get this unit test to pass!
     * - Create a [TransactionBuilder] and pass it a notary reference.
     * -- A notary [Party] object can be obtained from [FlowLogic.serviceHub.networkMapCache].
     * -- In this training project there is only one notary
     * - Create an [IOUContract.Commands.Issue] inside a new [Command].
     * -- The required signers will be the same as the state's participants
     * -- Add the [Command] to the transaction builder [addCommand].
     * - Use the flow's [IOUState] parameter as the output state with [addOutputState]
     * - Extra credit: use [TransactionBuilder.withItems] to create the transaction instead
     * - Sign the transaction and convert it to a [SignedTransaction] using the [serviceHub.signInitialTransaction] method.
     * - Return the [SignedTransaction].
     */
    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
        val lender = a.info.chooseIdentityAndCert().party
        val borrower = b.info.chooseIdentityAndCert().party
        val iou = IOUState(10.POUNDS, lender, borrower)
        val flow = IOUIssueFlow(iou)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        // Return the unsigned(!) SignedTransaction object from the IOUIssueFlow.
        val ptx: SignedTransaction = future.getOrThrow()
        // Print the transaction for debugging purposes.
        println(ptx.tx)
        // Check the transaction is well formed...
        // No outputs, one input IOUState and a command with the right properties.
        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.single().data is IOUState)
        val command = ptx.tx.commands.single()
        assert(command.value is IOUContract.Commands.Issue)
        assert(command.signers.toSet() == iou.participants.map { it.owningKey }.toSet())
        ptx.verifySignaturesExcept(
            borrower.owningKey,
            mockNetwork.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey
        )
    }

    /**
     * Task 2.
     * Now we have a well formed transaction, we need to properly verify it using the [IOUContract].
     * TODO: Amend the [IOUIssueFlow] to verify the transaction as well as sign it.
     * Hint: You can verify on the builder directly prior to finalizing the transaction. This way
     * you can confirm the transaction prior to making it immutable with the signature.
     */
    @Test
    fun flowReturnsVerifiedPartiallySignedTransaction() {
        // Check that a zero amount IOU fails.
        val lender = a.info.chooseIdentityAndCert().party
        val borrower = b.info.chooseIdentityAndCert().party
        val zeroIou = IOUState(0.POUNDS, lender, borrower)
        val futureOne = a.startFlow(IOUIssueFlow(zeroIou))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException> { futureOne.getOrThrow() }
        // Check that an IOU with the same participants fails.
        val borrowerIsLenderIou = IOUState(10.POUNDS, lender, lender)
        val futureTwo = a.startFlow(IOUIssueFlow(borrowerIsLenderIou))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException> { futureTwo.getOrThrow() }
        // Check a good IOU passes.
        val iou = IOUState(10.POUNDS, lender, borrower)
        val futureThree = a.startFlow(IOUIssueFlow(iou))
        mockNetwork.runNetwork()
        futureThree.getOrThrow()
    }

    /**
     * IMPORTANT: Review the [CollectSignaturesFlow] before continuing here.
     * Task 3.
     * Now we need to collect the signature from the [otherParty] using the [CollectSignaturesFlow].
     * TODO: Amend the [IOUIssueFlow] to collect the [otherParty]'s signature.
     * Hint:
     * On the Initiator side:
     * - Get a set of signers required from the participants who are not the node
     * - - [ourIdentity] will give you the identity of the node you are operating as
     * - Use [initiateFlow] to get a set of [FlowSession] objects
     * - - Using [state.participants] as a base to determine the sessions needed is recommended. [participants] is on
     * - - the state interface so it is guaranteed to exist where [lender] and [borrower] are not.
     * - - Hint: [ourIdentity] will give you the [Party] that represents the identity of the initiating flow.
     * - Use [subFlow] to start the [CollectSignaturesFlow]
     * - Pass it a [SignedTransaction] object and [FlowSession] set
     * - It will return a [SignedTransaction] with all the required signatures
     * - The subflow performs the signature checking and transaction verification for you
     *
     * On the Responder side:
     * - Create a subclass of [SignTransactionFlow]
     * - Override [SignTransactionFlow.checkTransaction] to impose any constraints on the transaction
     *
     * Using this flow you abstract away all the back-and-forth communication required for parties to sign a
     * transaction.
     */
    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val lender = a.info.chooseIdentityAndCert().party
        val borrower = b.info.chooseIdentityAndCert().party
        val iou = IOUState(10.POUNDS, lender, borrower)
        val flow = IOUIssueFlow(iou)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    /**
     * Task 4.
     * Now we need to store the finished [SignedTransaction] in both counter-party vaults.
     * TODO: Amend the [IOUIssueFlow] by adding a call to [FinalityFlow].
     * Hint:
     * - As mentioned above, use the [FinalityFlow] to ensure the transaction is recorded in both [Party] vaults.
     * - Do not use the [BroadcastTransactionFlow]!
     * - The [FinalityFlow] determines if the transaction requires notarisation or not.
     * - We don't need the notary's signature as this is an issuance transaction without a timestamp. There are no
     *   inputs in the transaction that could be double spent! If we added a timestamp to this transaction then we
     *   would require the notary's signature as notaries act as a timestamping authority.
     */
    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val lender = a.info.chooseIdentityAndCert().party
        val borrower = b.info.chooseIdentityAndCert().party
        val iou = IOUState(10.POUNDS, lender, borrower)
        val flow = IOUIssueFlow(iou)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        println("Signed transaction hash: ${stx.id}")
        listOf(a, b).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${stx.id}")
            assertEquals(stx.id, txHash)
        }
    }
}
