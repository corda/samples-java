package net.corda.samples.notarychange

import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.samples.notarychange.flows.IssueFlow
import net.corda.samples.notarychange.flows.SettleFlow
import net.corda.samples.notarychange.flows.SwitchNotaryFlow
import net.corda.testing.node.*
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.util.concurrent.ExecutionException
import kotlin.test.assertFailsWith

@Rule
val exception = ExpectedException.none()

class FlowTests {

    private var network: MockNetwork? = null
    private var a: StartedMockNode? = null
    private var b: StartedMockNode? = null

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.notarychange.contracts"),
                TestCordapp.findCordapp("net.corda.samples.notarychange.flows")
        ), notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("NotaryA", "London", "GB")),
                MockNetworkNotarySpec(CordaX500Name("NotaryB", "Toronto", "CA")))))
        a = network!!.createPartyNode(null)
        b = network!!.createPartyNode(null)
        network!!.runNetwork()
    }

    @After
    fun tearDown() {
        network!!.stopNodes()
    }



    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun NoNotaryChangesExpectToFail() {
        val issueflow = IssueFlow.Initiator(20, b!!.info.legalIdentities[0])
        val future = a!!.startFlow(issueflow)
        network!!.runNetwork()
        val returnString = future.get()
        println("\n----------")
        println(returnString)
        val id = returnString.substring(returnString.indexOf("linearId: ") + 10)
        println(id)
        val settleflow = SettleFlow.Initiator(fromString(id),
                network!!.notaryNodes[1].info.legalIdentities[0])
        val future2 = b!!.startFlow(settleflow)
        network!!.runNetwork()
        assertFailsWith<IllegalArgumentException> { future2.getOrThrow() }
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun CondunctNotaryChanges() {
        val issueflow = IssueFlow.Initiator(20, b!!.info.legalIdentities[0])
        val future = a!!.startFlow(issueflow)
        network!!.runNetwork()
        val returnString = future.get()
        println("\n----------")
        println(returnString)
        val id = returnString.substring(returnString.indexOf("linearId: ") + 10)
        println(id)

        //notary change
        val notarychange = SwitchNotaryFlow(fromString(id),
                network!!.notaryNodes[1].info.legalIdentities[0])
        val future3 = b!!.startFlow(notarychange)
        network!!.runNetwork()

        //settle
        val settleflow = SettleFlow.Initiator(fromString(id),
                network!!.notaryNodes[1].info.legalIdentities[0])
        val future2 = b!!.startFlow(settleflow)
        network!!.runNetwork()
        future2.get()
    }


}