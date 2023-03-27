package net.corda.samples.tokentofriend

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import org.junit.After
import org.junit.Before
import org.junit.Test
import net.corda.samples.tokentofriend.flows.CreateMyToken
import net.corda.samples.tokentofriend.flows.IssueToken
import net.corda.samples.tokentofriend.states.CustomTokenState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.*
import java.util.*
import kotlin.test.assertEquals

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode
    private lateinit var c: StartedMockNode
    private lateinit var d: StartedMockNode
    private lateinit var e: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.tokentofriend.contracts"),
                TestCordapp.findCordapp("net.corda.samples.tokentofriend.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
        ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ))
        a = network.createPartyNode()
        b = network.createPartyNode()
        c = network.createPartyNode()
        d = network.createPartyNode()
        e = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }
    @Test
    fun `Check the correct message is stored`() {

        val msg = "Test Message"
        val flow = CreateMyToken("Thomas@gmail.com","peter@gmail.com", msg)
        val future = a.startFlow(flow)
        network.runNetwork()
        val tokenStateID = future.getOrThrow()
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(tokenStateID.id),status = Vault.StateStatus.UNCONSUMED)
        val storedState = a.services.vaultService.queryBy(CustomTokenState::class.java, criteria = inputCriteria).states.single().state.data
        assertEquals(storedState.message, msg)
    }

    @Test
    fun `Check if non fungible token correctly created`(){
        val msg = "Token Creation"
        val createTokenflow = CreateMyToken("Thomas@gmail.com","peter@gmail.com", msg)
        val future = a.startFlow(createTokenflow)
        network.runNetwork()
        val tokenStateID = future.getOrThrow()
        val issueTokenflow = IssueToken(tokenStateID.id.toString())
        val future2 = a.startFlow(issueTokenflow)
        network.runNetwork()
        val resultString = future2.getOrThrow()
        println(resultString)
        val subString = resultString.indexOf("Token Id is: ")
        val nonfungibleTokenId = resultString.substring(subString+13,resultString.indexOf("Storage Node is:")-1)
        println("-"+ nonfungibleTokenId+"-")
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(UUID.fromString(nonfungibleTokenId)),status = Vault.StateStatus.UNCONSUMED)
        val storedNonFungibleTokenb = b.services.vaultService.queryBy(NonFungibleToken::class.java, criteria = inputCriteria).states
        val storedNonFungibleTokenc = c.services.vaultService.queryBy(NonFungibleToken::class.java, criteria = inputCriteria).states
        val storedNonFungibleTokend = d.services.vaultService.queryBy(NonFungibleToken::class.java, criteria = inputCriteria).states
        val storedNonFungibleTokene = e.services.vaultService.queryBy(NonFungibleToken::class.java, criteria = inputCriteria).states

        val storedToken = listOf(storedNonFungibleTokenb,storedNonFungibleTokenc,storedNonFungibleTokend,storedNonFungibleTokene)
                .filter { it.isNotEmpty() }.single().single().state.data
        println("-"+ storedToken.linearId+"-")
        assertEquals(storedToken.linearId, UniqueIdentifier(id = UUID.fromString(nonfungibleTokenId)))

    }




}