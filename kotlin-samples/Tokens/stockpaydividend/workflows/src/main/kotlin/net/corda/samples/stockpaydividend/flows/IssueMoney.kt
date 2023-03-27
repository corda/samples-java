package net.corda.samples.stockpaydividend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@StartableByRPC
class IssueMoney(val currency: String,
                 val amount: Long,
                 val recipient: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {

        // Create an instance of the fiat currency token type
        val token = getInstance(currency)

        // Create an instance of IssuedTokenType which represents the token is issued by this party
        val issuedTokenType = IssuedTokenType(ourIdentity, token)

        // Create an instance of FungibleToken for the fiat currency to be issued
        val fungibleToken = FungibleToken(Amount(amount, issuedTokenType), recipient, null)

        // Use the build-in flow, IssueTokens, to issue the required amount to the the recipient
        val stx = subFlow(IssueTokens(listOf(fungibleToken), listOf(recipient)))
        return ("\nIssued to " + recipient.name.organisation + " " + this.amount + " "
                + this.currency + " for stock issuance." + "\nTransaction ID: " + stx.id)
    }
}
