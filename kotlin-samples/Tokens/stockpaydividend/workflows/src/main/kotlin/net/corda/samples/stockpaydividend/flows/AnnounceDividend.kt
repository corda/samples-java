package net.corda.samples.stockpaydividend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlow
import com.r3.corda.lib.tokens.workflows.flows.evolvable.UpdateEvolvableTokenFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.IdentityService
import net.corda.samples.stockpaydividend.flows.utilities.QueryUtilities
import net.corda.samples.stockpaydividend.states.StockState
import java.math.BigDecimal
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class AnnounceDividend(val symbol: String,
                       val dividendPercentage: BigDecimal,
                       val executionDate: Date,
                       val payDate: Date) : FlowLogic<String>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String { // Retrieved the unconsumed StockState from the vault
        val stockStateRef: StateAndRef<StockState> = QueryUtilities.queryStock(symbol, serviceHub)
        val stock: StockState = stockStateRef.state.data

        // Form the output state here with a dividend to be announced
        val outputState = stock.copy(dividend = dividendPercentage, exDate = executionDate, payDate = payDate)

        // Get predefined observers
        val identityService = serviceHub.identityService
        val observers: List<Party> = getObserverLegalIdenties(identityService)!!
        val obSessions: MutableList<FlowSession> = ArrayList()
        for (observer in observers) {
            obSessions.add(initiateFlow(observer))
        }
        // Update the stock state and send a copy to the observers eventually
        val stx = subFlow(UpdateEvolvableTokenFlow(stockStateRef, outputState, listOf(), obSessions))
        subFlow(UpdateDistributionListFlow(stx))
        return "Stock ${symbol} has changed dividend percentage to ${dividendPercentage}. ${stx.id}"
    }
}

@InitiatedBy(AnnounceDividend::class)
class AnnounceDividendResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call():Unit {
        // To implement the responder flow, simply call the subflow of UpdateEvolvableTokenFlowHandler
        return subFlow(UpdateEvolvableTokenFlowHandler(counterpartySession))
    }
}

fun getObserverLegalIdenties(identityService: IdentityService): List<Party>? {
    var observers: MutableList<Party> = ArrayList()
    for (observerName in listOf("Observer", "Shareholder")) {
        val observerSet = identityService.partiesFromName(observerName!!, false)
        if (observerSet.size != 1) {
            val errMsg = String.format("Found %d identities for the observer.", observerSet.size)
            throw IllegalStateException(errMsg)
        }
        observers.add(observerSet.iterator().next())
    }
    return observers
}