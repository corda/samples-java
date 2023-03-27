package net.corda.samples.stockpaydividend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.flows.SignTransactionFlow.Companion.tracker
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.samples.stockpaydividend.contracts.DividendContract
import net.corda.samples.stockpaydividend.flows.utilities.QueryUtilities
import net.corda.samples.stockpaydividend.states.DividendState
import net.corda.samples.stockpaydividend.states.StockState
import java.math.BigDecimal
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class ClaimDividendReceivable(val symbol: String) : FlowLogic<String>() {
    @Suspendable
    override fun call(): String { // Retrieve the stock and pointer
        val stockPointer: TokenPointer<*> = QueryUtilities.queryStockPointer(symbol, serviceHub)
        val stockStateRef: StateAndRef<StockState> = stockPointer.pointer.resolve(serviceHub) as StateAndRef<StockState>
        val stockState: StockState = stockStateRef.state.data

        // Query the current Stock amount from shareholder
        val stockAmount = serviceHub.vaultService.tokenBalance(stockPointer)

        // Prepare to send the stock amount to the company to request dividend issuance
        val stockToClaim = ClaimNotification(stockAmount)
        val session = initiateFlow(stockState.issuer)

        // First send the stock state as which stock state the shareholder is referring to
        subFlow(SendStateAndRefFlow(session, listOf(stockStateRef)))

        // Then send the stock amount
        session.send(stockToClaim)

        // Wait for the transaction from the company, and sign it after the checking
        class SignTxFlow(otherPartyFlow: FlowSession,
                         progressTracker: ProgressTracker) : SignTransactionFlow(otherPartyFlow, progressTracker) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                requireThat<Any?> {
                    // Any checkings that the DividendContract is be able to validate. Below are some example constraints
                    val dividend: DividendState = stx.tx.outputsOfType(DividendState::class.java)[0]
                    "Claimed dividend should be owned by Shareholder".using(dividend.shareholder == ourIdentity)
                }
            }
        }
        val signTxFlow = SignTxFlow(session, tracker())

        // Checks if the later transaction ID of the received FinalityFlow is the same as the one just signed
        val txId = subFlow(signTxFlow).id
        subFlow(ReceiveFinalityFlow(session, txId))

        return "\nRequest has been sent, Please wait for the stock issuer to respond. $txId"
    }
}

@InitiatedBy(ClaimDividendReceivable::class)
class ClaimDividendReceivableResponder(private val holderSession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction { // Receives shareholder's state for input and output

        val holderStockStates: List<StateAndRef<StockState>> = subFlow(ReceiveStateAndRefFlow(holderSession))
        val holderStockState: StateAndRef<StockState> = holderStockStates[0]
        val stockState: StockState = holderStockState.state.data

        // Query the stored state of the company
        val stockPointer: TokenPointer<*> = QueryUtilities.queryStockPointer(stockState.symbol, serviceHub)
        val stockStateRef: StateAndRef<StockState> = stockPointer.pointer.resolve(serviceHub) as StateAndRef<StockState>

        // Receives the amount that the shareholder holds
        val claimNoticication:ClaimNotification = holderSession.receive(ClaimNotification::class.java).unwrap { it: ClaimNotification->
            if(holderStockState.ref.txhash != stockStateRef.ref.txhash){
                throw FlowException("StockState does not match with the issuers. Shareholder may not have updated the newest stock state.")
            }else{
                it
            }
        }

        // Preparing the token type of the paying fiat currency
        val currency: Currency = Currency.getInstance(stockState.currency)
        val dividendTokenType = TokenType(currency.currencyCode, currency.defaultFractionDigits)

        // Calculate the actual dividend paying to the shareholder
        val yield: BigDecimal = stockState.dividend.multiply(BigDecimal.valueOf(claimNoticication.amount.quantity))
        val dividend = `yield`.multiply(stockState.price).multiply(BigDecimal.valueOf(
                Math.pow(10.0, currency.defaultFractionDigits.toDouble())))

        // Create the dividend state
        val dividendAmount: Amount<TokenType> = Amount(dividend.longValueExact(), dividendTokenType)
        val outputDividend = DividendState(ourIdentity, holderSession.counterparty, Date(), dividendAmount, false, UniqueIdentifier())

        // Start building transaction
        // Using the notary from the previous transaction (dividend issuance)
        val notary = holderStockState.state.notary
        val txBuilder = TransactionBuilder(notary)
        // Build transaction Add creation of dividend with a reference of the shareholder stock state
        txBuilder
                .addOutputState(outputDividend, DividendContract.ID)
                .addReferenceState(ReferencedStateAndRef(holderStockState))
                .addCommand(DividendContract.Commands.Create(), listOf(ourIdentity.owningKey, holderSession.counterparty.owningKey))

        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)
        val sessions = listOf(holderSession)
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))
        return subFlow(FinalityFlow(stx, sessions))
    }

}


@CordaSerializable
class ClaimNotification(val amount: Amount<TokenType>)