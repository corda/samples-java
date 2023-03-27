package net.corda.samples.avatar

import net.corda.core.concurrent.CordaFuture
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.node.services.Vault.StateStatus
import net.corda.samples.avatar.flows.CreateAvatar
import net.corda.samples.avatar.flows.TransferAvatar
import net.corda.samples.avatar.states.Avatar


class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.avatar.contracts"),
                TestCordapp.findCordapp("net.corda.samples.avatar.flows")
        )))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }
    @Test
    fun `Create Avatar Test`() {
        val flow = CreateAvatar( "PETER-7526", 3)
        val future: CordaFuture<SignedTransaction?> = a.startFlow(flow)
        network.runNetwork()

        //successful query means the state is stored at node b's vault. Flow went through.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val state = a.services.vaultService.queryBy(Avatar::class.java, inputCriteria).states[0].state.data
    }

    @Test
    fun `Transfer Avatar Test`() {
        val createflow = CreateAvatar( "PETER-7526", 3)
        val future: CordaFuture<SignedTransaction?> = a.startFlow(createflow)
        network.runNetwork()

        val transferflow = TransferAvatar( "PETER-7526", b.info.legalIdentities[0].name.organisation)
        val future2: CordaFuture<SignedTransaction?> = a.startFlow(transferflow)
        network.runNetwork()

        //successful query means the state is stored at node b's vault. Flow went through.
        val inputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)
        val state = b.services.vaultService.queryBy(Avatar::class.java, inputCriteria).states[0].state.data
    }
}