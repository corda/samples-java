package com.tutorial

import com.google.common.collect.ImmutableList
import com.tutorial.flows.CreateAndIssueAppleStampInitiator
import com.tutorial.flows.PackApplesInitiator
import com.tutorial.flows.RedeemApplesInitiator
import com.tutorial.states.AppleStamp
import com.tutorial.states.BasketOfApples
import net.corda.core.contracts.UniqueIdentifier
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
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

class RedeemApplesWithStampTest {
    private var network: MockNetwork? = null
    private var a: StartedMockNode? = null
    private var b: StartedMockNode? = null
    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters().withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.tutorial.contracts"),
                TestCordapp.findCordapp("com.tutorial.flows"))))
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
    fun buyerRedeemBasketOfApples() {
        //Create Basket of Apples
        val createBasketOfApples = PackApplesInitiator("Fuji4072", 10)
        val future: Future<SignedTransaction> = a!!.startFlow(createBasketOfApples)
        network!!.runNetwork()

        //Issue Apple Stamp
        val issueAppleStamp = CreateAndIssueAppleStampInitiator(
                "Fuji4072", b!!.info.legalIdentities[0])
        val future1: Future<SignedTransaction> = a!!.startFlow(issueAppleStamp)
        network!!.runNetwork()
        val issuedStamp = future1.get().tx.outputStates[0] as AppleStamp
        val id = issuedStamp.linearId

        //Redeem Basket of Apples with stamp
        val redeemApples = RedeemApplesInitiator(b!!.info.legalIdentities[0], id)
        val future2: Future<SignedTransaction> = a!!.startFlow(redeemApples)
        network!!.runNetwork()

        //successful query means the state is stored at node b's vault. Flow went through.
        val outputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val state = b!!.services.vaultService
                .queryBy(BasketOfApples::class.java, outputCriteria).states[0].state.data
        assert(state.description == "Fuji4072")
    }
}