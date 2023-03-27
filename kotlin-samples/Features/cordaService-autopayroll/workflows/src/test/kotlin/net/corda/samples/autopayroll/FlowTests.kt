package net.corda.samples.autopayroll

import net.corda.samples.autopayroll.flows.RequestFlowInitiator
import net.corda.samples.autopayroll.flows.RequestFlowResponder
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("net.corda.samples.autopayroll.contracts"),
        TestCordapp.findCordapp("net.corda.samples.autopayroll.flows")
    ),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
    ))
    private val a = network.createNode()
    private val b = network.createNode()
    private val bank = network.createNode(CordaX500Name("BankOperator", "Toronto", "CA"))


    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(RequestFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    //Test #1 check if the requestState is being sent to the bank operator behind the scene.
    @Test
    fun `requestStateSent`() {
        val future = a.startFlow(RequestFlowInitiator("500", b.info.legalIdentities.first()))
        network.runNetwork()
        val ptx = future.get()
        println("Signed transaction hash: ${ptx.id}")
        listOf(a, bank).map {
            it.services.validatedTransactions.getTransaction(ptx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            println("$txHash == ${ptx.id}")
            Assert.assertEquals(ptx.id, txHash)
        }
    }

}