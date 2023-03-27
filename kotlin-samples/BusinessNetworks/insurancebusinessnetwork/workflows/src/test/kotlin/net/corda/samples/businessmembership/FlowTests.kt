package net.corda.samples.businessmembership

import net.corda.bn.states.MembershipState
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.samples.businessmembership.flows.membershipFlows.CreateNetwork
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class FlowTests {
    private var network: MockNetwork? = null
    private var a: StartedMockNode? = null
    private var b: StartedMockNode? = null

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
                .withCordappsForAllNodes(listOf(
                TestCordapp.findCordapp("net.corda.samples.businessmembership.contracts"),
                TestCordapp.findCordapp("net.corda.samples.businessmembership.flows"),
                TestCordapp.findCordapp("net.corda.bn.flows"),
                TestCordapp.findCordapp("net.corda.bn.states"))))
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
    fun createNetworkTest() {
        val flow = CreateNetwork()
        val future: Future<String> = a!!.startFlow(flow)
        network!!.runNetwork()
        val resString: String = future.get()
        println(resString)
        val subString = resString.indexOf("NetworkID: ")
        val networkId = resString.substring(subString + 11)
        println("-$networkId-")
        val inputCriteria: QueryCriteria = QueryCriteria.LinearStateQueryCriteria().withStatus(Vault.StateStatus.UNCONSUMED)
        val (_, networkId1) = a!!.services.vaultService
                .queryBy(MembershipState::class.java, inputCriteria).states[0].state.data
        println(networkId1)
        assert(networkId1 == networkId)
    }

}