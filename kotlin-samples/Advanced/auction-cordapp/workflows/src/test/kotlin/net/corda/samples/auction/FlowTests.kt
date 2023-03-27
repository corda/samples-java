package net.corda.samples.auction

import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.samples.auction.flows.CreateAssetFlow
import net.corda.samples.auction.flows.CreateAuctionFlow
import net.corda.samples.auction.states.Asset
import net.corda.samples.auction.states.AuctionState
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ExecutionException

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.auction.contracts"),
                TestCordapp.findCordapp("net.corda.samples.auction.flows")),
                networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ))
        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun AccountCreation() {
        val flow = CreateAssetFlow("Test Asset", "Dummy Asset", "http://abc.com/dummy.png")
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTransaction = future.get()
        val asset = signedTransaction.tx.getOutput(0) as Asset
        Assert.assertNotNull(asset)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAuctionFlow() {
        val assetflow = CreateAssetFlow("Test Asset", "Dummy Asset", "http://abc.com/dummy.png")
        val future = a.startFlow(assetflow)
        network.runNetwork()
        val signedTransaction = future.get()
        val (linearId) = signedTransaction.tx.getOutput(0) as Asset
        val auctionFlow: CreateAuctionFlow = CreateAuctionFlow(Amount.parseCurrency("1000 USD"),
                linearId.id, LocalDateTime.ofInstant(Instant.now().plusMillis(30000), ZoneId.systemDefault()))
        val future1 = a.startFlow<SignedTransaction>(auctionFlow)
        network.runNetwork()
        val transaction = future1.get()
        val auctionState = transaction.tx.getOutput(0) as AuctionState
        Assert.assertNotNull(auctionState)
    }
}