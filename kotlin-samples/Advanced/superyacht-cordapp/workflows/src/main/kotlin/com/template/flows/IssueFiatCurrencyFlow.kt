package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FinalityFlow
import net.corda.core.transactions.SignedTransaction


// *********
// * Flows *
// *********

@StartableByRPC
class IssueFiatCurrencyFlow(
    private val currency: String,
    private val amount: Long,
    private val recipient: Party) : FlowLogic<SignedTransaction>() {
    companion object {
        object CREATE_FIAT_CURRENCY_TOKEN : ProgressTracker.Step("Create an instance of the fiat currency token.")
        object CREATE_ISSUE_TOKEN_TYPE : ProgressTracker.Step("Create an instance of IssuedTokenType for the fiat currency.")
        object CREATE_FUNGIBLE_TOKEN_TYPE : ProgressTracker.Step("Create an instance of FungibleToken for the fiat currency to be issued.")
        object FINALISING_TRANSACTION : ProgressTracker.Step("Signing and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            CREATE_FIAT_CURRENCY_TOKEN,
            CREATE_ISSUE_TOKEN_TYPE,
            CREATE_FUNGIBLE_TOKEN_TYPE,
            FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call():SignedTransaction {
        progressTracker.currentStep = CREATE_FIAT_CURRENCY_TOKEN
        /* Create an instance of the fiat currency token */
        val token = FiatCurrency.Companion.getInstance(currency)

        progressTracker.currentStep = CREATE_ISSUE_TOKEN_TYPE
        /* Create an instance of IssuedTokenType for the fiat currency */
        val issuedTokenType = token issuedBy ourIdentity

        progressTracker.currentStep = CREATE_FUNGIBLE_TOKEN_TYPE
        /* Create an instance of FungibleToken for the fiat currency to be issued */
        val fungibleToken = FungibleToken(Amount(amount,issuedTokenType),recipient)

        progressTracker.currentStep = FINALISING_TRANSACTION
        return subFlow(IssueTokens(listOf(fungibleToken), listOf(recipient)))
    }
}