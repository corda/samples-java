package net.corda.samples.obligation.flow

import net.corda.samples.obligation.states.IOUState
import net.corda.core.contracts.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.finance.*
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.node.MockNetwork
import net.corda.core.identity.Party
import net.corda.core.internal.packageName
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.utils.sumCash
import net.corda.finance.schemas.CashSchemaV1
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import net.corda.samples.obligation.contract.IOUContract
import net.corda.samples.obligation.flows.*
import org.junit.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Practical exercise instructions Flows part 3.
 * Uncomment the unit tests and use the hints + unit test body to complete the FLows such that the unit tests pass.
 */
class IOUSettleFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("net.corda.samples.obligation", "net.corda.finance.contracts.asset", CashSchemaV1::class.packageName),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b, c)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(IOUIssueFlowResponder::class.java) }
        startedNodes.forEach { it.registerInitiatedFlow(IOUSettleFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    /**
     * Issue an IOU on the ledger, we need to do this before we can transfer one.
     */
    private fun issueIou(iou: IOUState): SignedTransaction {
        val flow = IOUIssueFlow(iou)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    /**
     * Issue some on-ledger cash to ourselves, we need to do this before we can Settle an IOU.
     */
    private fun issueCash(amount: Amount<Currency>): Cash.State {
        val flow = SelfIssueCashFlow(amount)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    /**
     * Task 1.
     * The first task is to grab the [IOUState] for the given [linearId] from the vault, assemble a transaction
     * and sign it.
     * TODO: Grab the IOU for the given [linearId] from the vault, build and sign the settle transaction.
     * Hints:
     * - Use the code from the [IOUTransferFlow] to get the correct [IOUState] from the vault.
     * - You will need to use the [CashUtils.generateSpend] functionality of the vault to add the cash states and cash command
     *   to your transaction. The API is quite simple. It takes a reference to a [ServiceHub], the [TransactionBuilder],
     *   an [Amount], [PartyAndCertificate] representing out identity (sender) and the [Party] object for the recipient.
     *   The function will mutate your builder by adding the states and commands.
     * - You then need to produce the output [IOUState] by using the [IOUState.pay] function.
     * - Add the input [IOUState] [StateAndRef] and the new output [IOUState] to the transaction.
     * - Sign the transaction and return it.
     */
    @Test
    fun flowReturnsCorrectlyFormedPartiallySignedTransaction() {
        val stx = issueIou(IOUState(10.POUNDS, b.info.chooseIdentityAndCert().party, a.info.chooseIdentityAndCert().party))
        issueCash(5.POUNDS)
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val settleResult = future.getOrThrow()
        // Check the transaction is well formed...
        // One output IOUState, one input IOUState reference, input and output cash
        a.transaction {
            val ledgerTx = settleResult.toLedgerTransaction(a.services, false)
            assert(ledgerTx.inputs.size == 2)
            assert(ledgerTx.outputs.size == 2)
            val outputIou = ledgerTx.outputs.map { it.data }.filterIsInstance<IOUState>().single()
            assertEquals(
                    outputIou,
                    inputIou.pay(5.POUNDS))
            // Sum all the output cash. This is complicated as there may be multiple cash output states with not all of them
            // being assigned to the lender.
            val outputCashSum = ledgerTx.outputs
                    .map { it.data }
                    .filterIsInstance<Cash.State>()
                    .filter { it.owner == b.info.chooseIdentityAndCert().party }
                    .sumCash()
                    .withoutIssuer()
            // Compare the cash assigned to the lender with the amount claimed is being settled by the borrower.
            assertEquals(
                    outputCashSum,
                    (inputIou.amount - inputIou.paid - outputIou.paid))
            val command = ledgerTx.commands.requireSingleCommand<IOUContract.Commands>()
            assert(command.value == IOUContract.Commands.Settle())
            // Check the transaction has been signed by the borrower.
            settleResult.verifySignaturesExcept(b.info.chooseIdentityAndCert().party.owningKey,
                    mockNetwork.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey)
        }
    }

    /**
     * Task 2.
     * Only the borrower should be running this flow for a particular IOU.
     * TODO: Grab the IOU for the given [linearId] from the vault and check the node running the flow is the borrower.
     * Hint: Use the data within the iou obtained from the vault to check the right node is running the flow.
     */
    @Test
    fun settleFlowCanOnlyBeRunByBorrower() {
        val stx = issueIou(IOUState(10.POUNDS, b.info.chooseIdentityAndCert().party, a.info.chooseIdentityAndCert().party))
        issueCash(5.POUNDS)
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
        val future = b.startFlow(flow)
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }

    /**
     * Task 3.
     * The borrower must have at least SOME cash in the right currency to pay the lender.
     * TODO: Add a check in the flow to ensure that the borrower has a balance of cash in the right currency.
     * Hint:
     * - Use [serviceHub.getCashBalances] - it is a map which can be queried by [Currency].
     * - Use an if statement to check there is cash in the right currency present.
     */
    @Test
    fun borrowerMustHaveCashInRightCurrency() {
        val stx = issueIou(IOUState(10.POUNDS, b.info.chooseIdentityAndCert().party, a.info.chooseIdentityAndCert().party))
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException>("Borrower has no GBP to settle.") { future.getOrThrow() }
    }

    /**
     * Task 4.
     * The borrower must have enough cash in the right currency to pay the lender.
     * TODO: Add a check in the flow to ensure that the borrower has enough cash to pay the lender.
     * Hint: Add another if statement similar to the one required above.
     */
    @Test
    fun borrowerMustHaveEnoughCashInRightCurrency() {
        val stx = issueIou(IOUState(10.POUNDS, b.info.chooseIdentityAndCert().party, a.info.chooseIdentityAndCert().party))
        issueCash(1.POUNDS)
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException>("Borrower has only 1.00 GBP but needs 5.00 GBP to settle.") { future.getOrThrow() }
    }

    /**
     * Task 5.
     * We need to get the transaction signed by the other party.
     * TODO: Use a subFlow call to [initateFlow] and the [SignTransactionFlow] to get a signature from the lender.
     */
    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val stx = issueIou(IOUState(10.POUNDS, b.info.chooseIdentityAndCert().party, a.info.chooseIdentityAndCert().party))
        issueCash(5.POUNDS)
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val settleResult = future.getOrThrow()
        // Check the transaction is well formed...
        // One output IOUState, one input IOUState reference, input and output cash
        settleResult.verifySignaturesExcept(mockNetwork.defaultNotaryNode.info.legalIdentitiesAndCerts.first().owningKey)
    }

    /**
     * Task 6.
     * We need to get the transaction signed by the notary service
     * TODO: Use a subFlow call to the [FinalityFlow] to get a signature from the lender.
     */
    @Test
    fun flowReturnsCommittedTransaction() {
        val stx = issueIou(IOUState(10.POUNDS, b.info.chooseIdentityAndCert().party, a.info.chooseIdentityAndCert().party))
        issueCash(5.POUNDS)
        val inputIou = stx.tx.outputs.single().data as IOUState
        val flow = IOUSettleFlow(inputIou.linearId, 5.POUNDS)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        val settleResult = future.getOrThrow()
        // Check the transaction is well formed...
        // One output IOUState, one input IOUState reference, input and output cash
        settleResult.verifyRequiredSignatures()
    }
}
