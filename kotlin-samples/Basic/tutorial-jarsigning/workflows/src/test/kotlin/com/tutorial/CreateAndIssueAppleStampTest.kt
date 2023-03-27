package com.tutorial

import com.google.common.collect.ImmutableList
import com.tutorial.flows.CreateAndIssueAppleStampInitiator
import com.tutorial.flows.Initiator
import com.tutorial.states.AppleStamp
import com.tutorial.states.TemplateState
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

class CreateAndIssueAppleStampTest {

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
    fun dummyTest() {
        val flow = Initiator(b!!.info.legalIdentities[0])
        val future: Future<SignedTransaction> = a!!.startFlow(flow)
        network!!.runNetwork()

        //successful query means the state is stored at node b's vault. Flow went through.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val state = b!!.services.vaultService
                .queryBy(TemplateState::class.java, inputCriteria).states[0].state.data
    }

    @Test
    fun CreateAndIssueAppleStampTest() {
        val flow1 = CreateAndIssueAppleStampInitiator(
                "HoneyCrispy 4072", b!!.info.legalIdentities[0])
        val future1: Future<SignedTransaction> = a!!.startFlow(flow1)
        network!!.runNetwork()

        //successful query means the state is stored at node b's vault. Flow went through.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria()
                .withStatus(StateStatus.UNCONSUMED)
        val state = b!!.services.vaultService
                .queryBy(AppleStamp::class.java, inputCriteria).states[0].state.data
        assert(state.stampDesc == "HoneyCrispy 4072")
    }
}