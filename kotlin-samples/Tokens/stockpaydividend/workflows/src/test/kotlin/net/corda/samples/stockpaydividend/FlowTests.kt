package net.corda.samples.stockpaydividend

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NetworkParameters
import net.corda.samples.stockpaydividend.flows.*
import net.corda.samples.stockpaydividend.states.DividendState
import net.corda.samples.stockpaydividend.states.StockState
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutionException

class FlowTests {
    protected var network: MockNetwork? = null
    protected var company: StartedMockNode? = null
    protected var observer: StartedMockNode? = null
    protected var shareholder: StartedMockNode? = null
    protected var bank: StartedMockNode? = null
    protected var exDate: Date? = null
    protected var payDate: Date? = null

    protected var notary: StartedMockNode? = null
    protected var notaryParty: Party? = null

    var COMPANY = TestIdentity(CordaX500Name("Company", "TestVillage", "US"))
    var SHAREHOLDER = TestIdentity(CordaX500Name("Shareholder", "TestVillage", "US"))
    var BANK = TestIdentity(CordaX500Name("Bank", "Rulerland", "US"))
    var OBSERVER = TestIdentity(CordaX500Name("Observer", "Rulerland", "US"))

    val STOCK_SYMBOL = "TEST"
    val STOCK_NAME = "Test Stock"
    val STOCK_CURRENCY = "USD"
    val STOCK_PRICE = BigDecimal.valueOf(7.4)
    val ISSUING_STOCK_QUANTITY = 2000
    val BUYING_STOCK = java.lang.Long.valueOf(500)
    val ISSUING_MONEY = 5000000
    val ANNOUNCING_DIVIDEND = BigDecimal("0.03")

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("net.corda.samples.stockpaydividend.contracts"),
                TestCordapp.findCordapp("net.corda.samples.stockpaydividend.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
        ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4)))

        company = network!!.createPartyNode(COMPANY.name)
        observer = network!!.createPartyNode(OBSERVER.name)
        shareholder = network!!.createPartyNode(SHAREHOLDER.name)
        bank = network!!.createPartyNode(BANK.name)
        notary = network!!.notaryNodes[0]
        notaryParty = notary!!.info.legalIdentities[0]

        // Set execution date as tomorrow
        val c = Calendar.getInstance()
        c.add(Calendar.DATE, 1)
        exDate = c.time

        // Set pay date as the day after tomorrow
        c.add(Calendar.DATE, 1)
        payDate = c.time
        network!!.startNodes()
    }

    @After
    fun tearDown() {
        network!!.stopNodes()
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun issueTest() {
        // Issue Stock
        val future = company!!.startFlow(CreateAndIssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty!!))
        network!!.runNetwork()
        val stx = future.get()
        val stxID = stx.substring(stx.lastIndexOf(" ") + 1)
        val stxIDHash: SecureHash = SecureHash.parse(stxID)

        //Check if company and observer of the stock have recorded the transactions
        val issuerTx = company!!.services.validatedTransactions.getTransaction(stxIDHash)
        val observerTx = observer!!.services.validatedTransactions.getTransaction(stxIDHash)
        Assert.assertNotNull(issuerTx)
        Assert.assertNotNull(observerTx)
        Assert.assertEquals(issuerTx, observerTx)
    }


    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun moveTest() {
        // Issue Stock
        var future = company!!.startFlow<String?>(CreateAndIssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty!!))
        network!!.runNetwork()
        future.get()

        // Move Stock
        future = company!!.startFlow(MoveStock(STOCK_SYMBOL, BUYING_STOCK, shareholder!!.info.legalIdentities[0]))
        network!!.runNetwork()
        val moveTx = future.get()

        //Retrieve states from receiver
        val receivedStockStatesPages = shareholder!!.services.vaultService.queryBy(StockState::class.java).states
        val receivedStockState = receivedStockStatesPages[0].state.data
        val (quantity) = shareholder!!.services.vaultService.tokenBalance(receivedStockState.toPointer(receivedStockState.javaClass))

        //Check
        Assert.assertEquals(quantity, java.lang.Long.valueOf(500).toLong())

        //Retrieve states from sender
        val remainingStockStatesPages = company!!.services.vaultService.queryBy(StockState::class.java).states
        val remainingStockState = remainingStockStatesPages[0].state.data
        val (quantity1) = company!!.services.vaultService.tokenBalance(remainingStockState.toPointer(remainingStockState.javaClass))

        //Check
        Assert.assertEquals(quantity1, java.lang.Long.valueOf(1500).toLong())
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun announceDividendTest() {
        // Issue Stock
        var future = company!!.startFlow(
                CreateAndIssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty!!))
        network!!.runNetwork()
        future.get()

        // Move Stock
        future = company!!.startFlow(MoveStock(STOCK_SYMBOL, BUYING_STOCK, shareholder!!.info.legalIdentities[0]))
        network!!.runNetwork()
        future.get()

        // Announce Dividend
        future = company!!.startFlow(AnnounceDividend(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate!!, payDate!!))
        network!!.runNetwork()
        val announceTxSting = future.get()
        val stxID = announceTxSting.substring(announceTxSting.lastIndexOf(" ") + 1)
        val announceTx: SecureHash = SecureHash.parse(stxID)

        // Retrieve states from sender
        val remainingStockStatesPages = company!!.services.vaultService.queryBy(StockState::class.java).states
        val (_, _, _, _, _, dividend, exDate, payDate) = remainingStockStatesPages[0].state.data
        Assert.assertEquals(dividend, ANNOUNCING_DIVIDEND)
        Assert.assertEquals(exDate, exDate)
        Assert.assertEquals(payDate, payDate)

        // Check observer has recorded the same transaction
        val issuerTx = company!!.services.validatedTransactions.getTransaction(announceTx)
        val observerTx = observer!!.services.validatedTransactions.getTransaction(announceTx)
        Assert.assertNotNull(issuerTx)
        Assert.assertNotNull(observerTx)
        Assert.assertEquals(issuerTx, observerTx)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun getStockUpdateTest() {
        // Issue Stock
        var future = company!!.startFlow<String?>(CreateAndIssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty!!))
        network!!.runNetwork()
        future.get()

        // Move Stock
        future = company!!.startFlow(MoveStock(STOCK_SYMBOL, BUYING_STOCK, shareholder!!.info.legalIdentities[0]))
        network!!.runNetwork()
        future.get()

        // Announce Dividend
        future = company!!.startFlow(AnnounceDividend(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate!!, payDate!!))
        network!!.runNetwork()
        future.get()

        // Get Stock Update
        future = shareholder!!.startFlow(ClaimDividendReceivable(STOCK_SYMBOL))
        network!!.runNetwork()
        future.get()

        // Checks if the shareholder actually receives the same transaction and updated the stock state (with new dividend)
        val issuerStockStateRefs = company!!.services.vaultService.queryBy(StockState::class.java).states
        val (txhash) = issuerStockStateRefs[0].ref
        val holderStockStateRefs = shareholder!!.services.vaultService.queryBy(StockState::class.java).states
        val (txhash1) = holderStockStateRefs[0].ref
        Assert.assertEquals(txhash, txhash1)
    }


    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun claimDividendTest() {
        // Issue Stock
        var future = company!!.startFlow(CreateAndIssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty!!))
        network!!.runNetwork()
        future.get()

        // Move Stock
        future = company!!.startFlow(MoveStock(STOCK_SYMBOL, BUYING_STOCK, shareholder!!.info.legalIdentities[0]))
        network!!.runNetwork()
        val moveTx = future.get()

        // Announce Dividend
        future = company!!.startFlow(AnnounceDividend(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate!!, payDate!!))
        network!!.runNetwork()
        future.get()

        // Shareholder claims Dividend
        future = shareholder!!.startFlow(ClaimDividendReceivable(STOCK_SYMBOL))
        network!!.runNetwork()
        val claimTxString = future.get()
        val stxID = claimTxString.substring(claimTxString.lastIndexOf(" ") + 1)
        val claimTxID: SecureHash = SecureHash.parse(stxID)

        // Checks if the dividend amount is correct
        val holderDividendPages = shareholder!!.services.vaultService.queryBy(DividendState::class.java).states
        val (_, _, _, dividendAmount) = holderDividendPages[0].state.data
        val fractionalDigit = dividendAmount.token.fractionDigits
        val yieldAmount = BigDecimal.valueOf(BUYING_STOCK).multiply(ANNOUNCING_DIVIDEND)
        val receivingDividend = yieldAmount.multiply(STOCK_PRICE).multiply(BigDecimal.valueOf(Math.pow(10.0, fractionalDigit.toDouble())))
        Assert.assertEquals(dividendAmount.quantity, receivingDividend.toLong())

        // Check company and shareholder owns the same transaction
        val issuerTx = company!!.services.validatedTransactions.getTransaction(claimTxID)
        val holderTx = shareholder!!.services.validatedTransactions.getTransaction(claimTxID)
        Assert.assertNotNull(issuerTx)
        Assert.assertNotNull(holderTx)
        Assert.assertEquals(issuerTx, holderTx)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun payDividendTest() {
        // Issue Money
        var future = bank!!.startFlow<String?>(IssueMoney(STOCK_CURRENCY, ISSUING_MONEY.toLong(), company!!.info.legalIdentities[0]))
        network!!.runNetwork()
        future.get()

        // Issue Stock
        future = company!!.startFlow(CreateAndIssueStock(STOCK_SYMBOL, STOCK_NAME, STOCK_CURRENCY, STOCK_PRICE, ISSUING_STOCK_QUANTITY, notaryParty!!))
        network!!.runNetwork()
        future.get()

        // Move Stock
        future = company!!.startFlow(MoveStock(STOCK_SYMBOL, BUYING_STOCK, shareholder!!.info.legalIdentities[0]))
        network!!.runNetwork()
        future.get()

        // Announce Dividend
        future = company!!.startFlow(AnnounceDividend(STOCK_SYMBOL, ANNOUNCING_DIVIDEND, exDate!!, payDate!!))
        network!!.runNetwork()
        future.get()

        // Shareholder claims Dividend
        future = shareholder!!.startFlow(ClaimDividendReceivable(STOCK_SYMBOL))
        network!!.runNetwork()
        future.get()

        //Pay Dividend
        val futurePayDiv = company!!.startFlow<List<String>>(PayDividend())
        network!!.runNetwork()
        val txList = futurePayDiv.get()

        // The above test should only have 1 transaction created
        Assert.assertEquals(txList.size.toLong(), 1)
        val payDivTxString = txList[0]
        val stxID = payDivTxString.substring(payDivTxString.lastIndexOf(" ") + 1)
        val payDivTxID: SecureHash = SecureHash.parse(stxID)

        // Checks if no Dividend state left unspent in shareholder's and company's vault
        val holderDivStateRefs = shareholder!!.services.vaultService.queryBy(DividendState::class.java).states
        assert(holderDivStateRefs.isEmpty())
        val issuerDivStateRefs = company!!.services.vaultService.queryBy(DividendState::class.java).states
        assert(issuerDivStateRefs.isEmpty())

        // Validates shareholder has received equivalent fiat currencies of the dividend
        val fiatTokenType = getInstance(STOCK_CURRENCY)
        val (quantity) = shareholder!!.services.vaultService.tokenBalance(fiatTokenType)
        val receivingDividend = BigDecimal.valueOf(BUYING_STOCK).multiply(STOCK_PRICE).multiply(ANNOUNCING_DIVIDEND)
        Assert.assertEquals(quantity, receivingDividend.movePointRight(2).toLong())

        // Check company and shareholder owns the same transaction
        val issuerTx = company!!.services.validatedTransactions.getTransaction(payDivTxID)
        val holderTx = shareholder!!.services.validatedTransactions.getTransaction(payDivTxID)
        Assert.assertNotNull(issuerTx)
        Assert.assertNotNull(holderTx)
        Assert.assertEquals(issuerTx, holderTx)
    }

}