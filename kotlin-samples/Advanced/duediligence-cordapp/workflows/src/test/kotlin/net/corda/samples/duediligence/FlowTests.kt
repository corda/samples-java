package net.corda.samples.duediligence

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault.RelevancyStatus
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.samples.duediligence.flows.RequestToValidateCorporateRecordsInitiator
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import java.util.*
import java.util.concurrent.Future


class FlowTests {

    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp("net.corda.samples.duediligence.contracts"),
                        TestCordapp.findCordapp("net.corda.samples.duediligence.flows")),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {

    }

    @Test
    fun `Auditing Request Creation test`() {
        val flow1 = RequestToValidateCorporateRecordsInitiator(b.info.legalIdentities[0], 10)
        val future = a.startFlow(flow1)
        network.runNetwork()
        val result1 = future.get()
        println(result1)
        val subString = result1?.indexOf("Case Id: ")
        val ApproalID = subString?.plus(9)?.let { result1?.substring(it) }
        println("-$ApproalID-")

        val id = ApproalID?.let { fromString(it) }
        //Query the input
        //Query the input
        val inputCriteria: QueryCriteria.LinearStateQueryCriteria = QueryCriteria.LinearStateQueryCriteria()
                .withUuid(Arrays.asList(UUID.fromString(id.toString())))
                .withStatus(StateStatus.UNCONSUMED)
                .withRelevancyStatus(RelevancyStatus.RELEVANT)
        val (state) = a.services.vaultService.queryBy<ContractState>(ContractState::class.java, inputCriteria).states.get(0)
        val result = state.data as CorporateRecordsAuditRequest
        assertEquals(result.linearId, id)
    }
}