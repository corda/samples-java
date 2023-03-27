package net.corda.samples.stockpaydividend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokensHandler
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.stockpaydividend.flows.utilities.QueryUtilities
import net.corda.samples.stockpaydividend.states.StockState

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class MoveStock(val symbol: String,
                val quantity: Long,
                val recipient: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        // To get the transferring stock, we can get the StockState from the vault and get it's pointer
        val stockPointer: TokenPointer<StockState> = QueryUtilities.queryStockPointer(symbol, serviceHub)

        // With the pointer, we can get the create an instance of transferring Amount
        // With the pointer, we can get the create an instance of transferring Amount
        val amount: Amount<TokenType> = Amount(quantity, stockPointer)

        //Use built-in flow for move tokens to the recipient
        //Use built-in flow for move tokens to the recipient
        val stx = subFlow<SignedTransaction>(MoveFungibleTokens(amount, recipient))
        return ("\nIssued " + quantity + " " + symbol + " stocks to "
                + recipient.name.organisation + ".\nTransaction ID: " + stx.id)    }
}

@InitiatedBy(MoveStock::class)
class MoveStockResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Simply use the MoveFungibleTokensHandler as the responding flow
        return subFlow(MoveFungibleTokensHandler(counterpartySession))
    }
}
