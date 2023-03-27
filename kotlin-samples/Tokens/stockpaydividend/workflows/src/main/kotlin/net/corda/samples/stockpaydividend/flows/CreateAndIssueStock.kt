package net.corda.samples.stockpaydividend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.IdentityService
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.stockpaydividend.states.StockState
import java.math.BigDecimal
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class CreateAndIssueStock(val symbol: String,
                          val name: String,
                          val currency: String,
                          val price: BigDecimal,
                          val issueVol: Int,
                          val notary: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {

        // Sample specific - retrieving the hard-coded observers
        val identityService = serviceHub.identityService
        val observers: List<Party> = getObserverLegalIdenties(identityService)!!

        // Construct the output StockState
        val stockState = StockState(ourIdentity, symbol,
                name, currency,
                price, BigDecimal.ZERO,  // A newly issued stock should not have any dividend
                Date(), Date(),
                UniqueIdentifier())

        // The notary provided here will be used in all future actions of this token
        val transactionState = stockState withNotary notary

        // Using the build-in flow to create an evolvable token type -- Stock
        subFlow(CreateEvolvableTokens(transactionState, observers))

        // Similar in IssueMoney flow, class of IssuedTokenType represents the stock is issued by the company party
        val issuedStock = stockState.toPointer(stockState.javaClass) issuedBy ourIdentity

        // Create an specified amount of stock with a pointer that refers to the StockState
        val issueAmount = Amount(issueVol.toLong(), issuedStock)

        // Indicate the recipient which is the issuing party itself here
        val stockToken = FungibleToken(issueAmount, ourIdentity, null)

        // Finally, use the build-in flow to issue the stock tokens. Observer parties provided here will record a copy of the transactions
        val stx = subFlow(IssueTokens(listOf(stockToken), observers))
        return ("\nGenerated " + issueVol + " " + symbol + " stocks with price: "
                + price + " " + currency + "\nTransaction ID: " + stx.id)
    }

    fun getObserverLegalIdenties(identityService: IdentityService): List<Party>? {
        var observers: MutableList<Party> = ArrayList()
        for (observerName in listOf("Observer")) {
            val observerSet = identityService.partiesFromName(observerName!!, false)
            if (observerSet.size != 1) {
                val errMsg = String.format("Found %d identities for the observer.", observerSet.size)
                throw IllegalStateException(errMsg)
            }
            observers.add(observerSet.iterator().next())
        }
        return observers
    }
}
