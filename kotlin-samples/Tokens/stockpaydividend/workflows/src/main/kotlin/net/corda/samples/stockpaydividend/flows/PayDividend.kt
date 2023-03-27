package net.corda.samples.stockpaydividend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.flows.SignTransactionFlow.Companion.tracker
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.stockpaydividend.contracts.DividendContract
import net.corda.samples.stockpaydividend.states.DividendState
import java.util.*
import java.util.stream.Collectors

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class PayDividend : FlowLogic<List<String>>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():List<String>  {
        //Query the vault for any unconsumed DividendState
        //Query the vault for any unconsumed DividendState
        val stateAndRefs: List<StateAndRef<DividendState>> = serviceHub.vaultService.queryBy<DividendState>().states

        val transactions: List<SignedTransaction> = ArrayList()
        var notes: MutableList<String> = ArrayList()
        //For each queried unpaid DividendState, pay off the dividend with the corresponding amount.
        for (result in stateAndRefs) {
            val dividendState: DividendState = result.state.data
            val shareholder: Party = dividendState.shareholder

            // Generate input and output pair of moving fungible tokens
            val fiatIoPair = DatabaseTokenSelection(serviceHub).generateMove(listOf(Pair(shareholder,dividendState.dividendAmount)),ourIdentity)

            // Using the notary from the previous transaction (dividend issuance)
            val notary = result.state.notary

            // Start building transaction
            val txBuilder = TransactionBuilder(notary)
            txBuilder
                    .addInputState(result)
                    .addCommand(DividendContract.Commands.Pay(),dividendState.participants.map { it.owningKey })

            // As a later part of TokenSelection.generateMove which generates a move of tokens handily
            addMoveTokens(txBuilder, fiatIoPair.first, fiatIoPair.second)

            // Verify the transactions with contracts
            txBuilder.verify(serviceHub)

            // Sign the transaction
            val ptx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)

            // Instantiate a network session with the shareholder
            val holderSession = initiateFlow(shareholder)
            val sessions = listOf(holderSession)

            // Ask the shareholder to sign the transaction
            val stx = subFlow(CollectSignaturesFlow(ptx, listOf(holderSession)))
            val fstx = subFlow<SignedTransaction>(FinalityFlow(stx, sessions))
            notes.add("\nPaid to " + dividendState.shareholder.name.organisation
                    .toString() + " " + (dividendState.dividendAmount.quantity / 100).toString() + " "
                    + dividendState.dividendAmount.token.tokenIdentifier + "\nTransaction ID: " + fstx.id)
        }
        return notes
    }
}

@InitiatedBy(PayDividend::class)
class PayDividendResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        // Override the SignTransactionFlow for custom checkings
        // Override the SignTransactionFlow for custom checkings
        class SignTxFlow (otherPartyFlow: FlowSession, progressTracker: ProgressTracker) : SignTransactionFlow(otherPartyFlow, progressTracker) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                requireThat<Any?> {
                    // Any checkings that the DividendContract is be not able to validate.
                    val outputFiats = stx.tx.outputsOfType(FungibleToken::class.java)
                    val holderFiats = outputFiats.stream().filter { fiat: FungibleToken -> fiat.holder.equals(ourIdentity) }.collect(Collectors.toList())
                    "One FungibleToken output should be held by Shareholder".using(holderFiats.size == 1)
                }
            }
        }
        // Wait for the transaction from the company, and sign it after the checking
        val signTxFlow = SignTxFlow(counterpartySession, tracker())
        // Checks if the later transaction ID of the received FinalityFlow is the same as the one just signed
        val txId = subFlow(signTxFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, txId))

    }
}
