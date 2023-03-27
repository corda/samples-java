package net.corda.samples.supplychain


import com.r3.corda.lib.accounts.workflows.services.AccountService
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.samples.supplychain.accountUtilities.CreateNewAccount
import net.corda.samples.supplychain.accountUtilities.ShareAccountTo
import net.corda.samples.supplychain.flows.SendInvoice
import net.corda.samples.supplychain.states.InvoiceState
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.supplychain.contracts"),
                TestCordapp.findCordapp("net.corda.samples.supplychain.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows")
        ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun AccountCreation() {
        val createAcct = CreateNewAccount("TestAccountA")
        val future: Future<String> = a.startFlow(createAcct)
        network.runNetwork()
        val accountService: AccountService = a.services.cordaService(KeyManagementBackedAccountService::class.java)
        val myAccount = accountService.accountInfo("TestAccountA")
        assert(myAccount.size != 0)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun InvoiceFlowTest() {
        val createAcct = CreateNewAccount("TestAccountA")
        val future: Future<String> = a.startFlow(createAcct)
        network.runNetwork()
        val shareAToB = ShareAccountTo("TestAccountA", b.info.legalIdentities[0])
        val future2: Future<String> = a.startFlow(shareAToB)
        network.runNetwork()
        val createAcct2 = CreateNewAccount("TestAccountB")
        val future3: Future<String> = b.startFlow(createAcct2)
        network.runNetwork()
        val shareBToA = ShareAccountTo("TestAccountB", a.info.legalIdentities[0])
        val future4: Future<String> = b.startFlow(shareBToA)
        network.runNetwork()
        val invoiceflow = SendInvoice("TestAccountA", "TestAccountB", 20)
        val future5: Future<String> = a.startFlow(invoiceflow)
        network.runNetwork()

        //retrieve
        val accountService: AccountService = b.services.cordaService(KeyManagementBackedAccountService::class.java)
        val (_, _, identifier) = accountService.accountInfo("TestAccountB")[0].state.data
        val criteria = VaultQueryCriteria()
                .withExternalIds(Arrays.asList(identifier.id))
        val storedState = b.services.vaultService.queryBy(InvoiceState::class.java, criteria).states[0].state.data
        assert(storedState.amount == 20)
    }


}