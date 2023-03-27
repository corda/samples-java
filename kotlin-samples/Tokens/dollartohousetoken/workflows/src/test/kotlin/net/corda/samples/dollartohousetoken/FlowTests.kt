package net.corda.samples.dollartohousetoken

import com.google.common.collect.ImmutableList
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.samples.dollartohousetoken.flows.CreateAndIssueHouseToken
import net.corda.samples.dollartohousetoken.states.HouseState
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.Future
import kotlin.test.assertEquals


class FlowTests {
    private var network: MockNetwork? = null
    private var a: StartedMockNode? = null
    private var b: StartedMockNode? = null

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.dollartohousetoken.contracts"),
                TestCordapp.findCordapp("net.corda.samples.dollartohousetoken.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
        ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ))
        a = network!!.createPartyNode(null)
        b = network!!.createPartyNode(null)
        network!!.runNetwork()
    }

    @After
    fun tearDown() {
        network!!.stopNodes()
    }

    @Test
    fun houseTokenStateCreation() {
        val createAndIssueFlow = CreateAndIssueHouseToken(b!!.info.legalIdentities[0],
                Amount.parseCurrency("1000 USD"), 10,
                "500 sqft", "NA", "NYC")
        val future: Future<String> = a!!.startFlow(createAndIssueFlow)
        network!!.runNetwork()
        val resultString = future.get()
        println(resultString)
        val subString = resultString.indexOf("UUID: ");
        val nonfungibleTokenId = resultString.substring(subString + 6, resultString.indexOf(". (This"))
        println("-" + nonfungibleTokenId + "-")
        val inputCriteria: QueryCriteria = LinearStateQueryCriteria().withUuid(Arrays.asList(UUID.fromString(nonfungibleTokenId))).withStatus(StateStatus.UNCONSUMED)
        val storedNonFungibleTokenb = b!!.services.vaultService.queryBy(HouseState::class.java, inputCriteria).states
        val (linearId) = storedNonFungibleTokenb[0].state.data
        println("-$linearId-")
        assertEquals(linearId.toString(), nonfungibleTokenId)
    }
}