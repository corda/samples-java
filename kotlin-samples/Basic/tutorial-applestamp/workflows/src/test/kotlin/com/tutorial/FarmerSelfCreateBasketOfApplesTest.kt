package com.tutorial

import com.google.common.collect.ImmutableList
import com.tutorial.flows.PackApplesInitiator
import com.tutorial.states.BasketOfApples
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Future

class FarmerSelfCreateBasketOfApplesTest {
    private var network: MockNetwork? = null
    private var a: StartedMockNode? = null
    private var b: StartedMockNode? = null

    @Before
    fun setup() {
        network = MockNetwork(
            MockNetworkParameters().withCordappsForAllNodes(
                ImmutableList.of(
                    TestCordapp.findCordapp("com.tutorial.contracts"),
                    TestCordapp.findCordapp("com.tutorial.flows")
                )
            )
        )
        a = network!!.createPartyNode(null)
        b = network!!.createPartyNode(null)
        network!!.runNetwork()
    }

    @After
    fun tearDown() {
        network!!.stopNodes()
    }

    @Test
    fun createBasketOfApples() {
        val flow1 = PackApplesInitiator("Fuji4072", 10)
        val future: Future<SignedTransaction> = a!!.startFlow(flow1)
        network!!.runNetwork()

        //successful query means the state is stored at node b's vault. Flow went through.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val state = a!!.services.vaultService
            .queryBy(BasketOfApples::class.java, inputCriteria).states[0].state.data
        println("-------------------------")
        println(state.owner)
        println("-------------------------")
        assert(state.description == "Fuji4072")
    }
}

