package net.corda.samples.obligation.flow

import net.corda.core.contracts.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.packageName
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.schemas.CashSchemaV1
import net.corda.samples.obligation.accountUtil.CreateNewAccountAndShare
import net.corda.samples.obligation.accountUtil.ViewAccounts
import net.corda.samples.obligation.accountUtil.ViewIOUByAccount
import net.corda.samples.obligation.flows.*
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * Practical exercise instructions Flows part 2.
 * Uncomment the unit tests and use the hints + unit test body to complete the Flows such that the unit tests pass.
 */
class IOUTransferFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(
            MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.obligation.flows"),
                TestCordapp.findCordapp("net.corda.samples.obligation.contract"),
                TestCordapp.findCordapp("net.corda.finance.contracts.asset"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                TestCordapp.findCordapp(CashSchemaV1::class.packageName)
            ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
            )
        )

        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b, c)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach { it.registerInitiatedFlow(IOUIssueFlowResponder::class.java) }
        startedNodes.forEach { it.registerInitiatedFlow(IOUTransferFlowResponder::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    private fun moneyDrop(acctID: UUID): SignedTransaction{
        val flow = MoneyDropFlow(acctID)
        val future = a.startFlow(flow)
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun TransferFlowTest() {
        val ax500 = a.info.legalIdentities[0]
        val bx500 = b.info.legalIdentities[0]
        val cx500 = c.info.legalIdentities[0]

        //Create account
        val createAcct = CreateNewAccountAndShare("bob6424",bx500,cx500)
        val future: Future<String> = a.startFlow(createAcct)
        mockNetwork.runNetwork()

        val createAcct2 = CreateNewAccountAndShare("Julie7465",ax500,cx500)
        val future2: Future<String> = b.startFlow(createAcct2)
        mockNetwork.runNetwork()

        val createAcct3 = CreateNewAccountAndShare("Peter9665",ax500,bx500)
        val future22: Future<String> = c.startFlow(createAcct3)
        mockNetwork.runNetwork()

        //View accounts
        val viewAcct = ViewAccounts()
        val future3: List<String> = a.startFlow(viewAcct).getOrThrow()
        println(future3)
        val startPos = future3[0].indexOf("id: ")
        val meID = future3[0].substring(startPos+4)
        val lenderID = future3[1].substring(startPos+6)
        val newlenderID = future3[2].substring(startPos+6)

        println("meID: ")
        println(meID)
        println("lenderID: ")
        println(lenderID)
        println("new - lenderID: ")
        println(newlenderID)

        //issue iou
        val iou = IOUIssueFlow(UUID.fromString(meID), UUID.fromString(lenderID),20)
        val future5 = a.startFlow(iou)
        mockNetwork.runNetwork()

        val iouID = future5.getOrThrow()
        println("IOU UUID = ")
        println(iouID)

        val checkIOU = ViewIOUByAccount("bob6424")
        val future7 = a.startFlow(checkIOU)
        mockNetwork.runNetwork()
        println(future7.getOrThrow())

        val transferIOU = IOUTransferFlow(UniqueIdentifier.fromString(iouID),UUID.fromString(lenderID), UUID.fromString(newlenderID))
        val future8 = b.startFlow(transferIOU)
        mockNetwork.runNetwork()
        println("-------------")
        println(future8.getOrThrow())
        println("-------------")

        val checkIOU2 = ViewIOUByAccount("bob6424")
        val future9 = a.startFlow(checkIOU2)
        mockNetwork.runNetwork()
        println(future9.getOrThrow())

    }
}
