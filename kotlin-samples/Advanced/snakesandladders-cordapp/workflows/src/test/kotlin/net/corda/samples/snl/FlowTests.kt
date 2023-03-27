package net.corda.samples.snl

import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.samples.snl.flows.CreateBoardConfig
import net.corda.samples.snl.flows.CreateGameFlow
import net.corda.samples.snl.states.BoardConfig
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.snl.contracts"),
                TestCordapp.findCordapp("net.corda.samples.snl.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
                TestCordapp.findCordapp("com.r3.corda.lib.ci")
        ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        )
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }


    @Test
    @Throws(Exception::class)
    fun testCreateBoardConfigFlow() {
        val createAccount1 = CreateAccount("Ashutosh")
        a.startFlow<StateAndRef<AccountInfo>>(createAccount1)
        network.runNetwork()
        val createAccount2 = CreateAccount("Peter")
        a.startFlow<StateAndRef<AccountInfo>>(createAccount2)
        network.runNetwork()
        val createBoardConfig = CreateBoardConfig.Initiator("Ashutosh", "Peter")
        val signedTransactionCordaFuture = a.startFlow(createBoardConfig)
        network.runNetwork()
        val signedTransaction = signedTransactionCordaFuture.get()
        val boardConfig = signedTransaction.tx.getOutput(0) as BoardConfig
        Assert.assertNotNull(boardConfig)
    }


    @Test
    @Throws(Exception::class)
    fun testCreateGameFlow() {
        val createAccount1 = CreateAccount("Ashutosh")
        a.startFlow<StateAndRef<AccountInfo>>(createAccount1)
        network.runNetwork()
        val createAccount2 = CreateAccount("Peter")
        a.startFlow<StateAndRef<AccountInfo>>(createAccount2)
        network.runNetwork()
        val createBoardConfig = CreateBoardConfig.Initiator("Ashutosh", "Peter")
        val signedTransactionCordaFuture = a.startFlow(createBoardConfig)
        network.runNetwork()
        val createGameFlow = CreateGameFlow.Initiator("Ashutosh", "Peter")
        val signedTransactionCordaFuture1 = a.startFlow(createGameFlow)
        network.runNetwork()
        val gameId = signedTransactionCordaFuture1.get()
        Assert.assertNotNull(gameId)
    }
}