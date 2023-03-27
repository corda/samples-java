package net.corda.samples.tokenizedhouse.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState
import java.math.BigDecimal

// *********
// * Flows *
// *********
@StartableByRPC
class CreateHouseTokenFlow(val symbol: String,
                           val valuationOfHouse:Int) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        //create token type
        val evolvableTokenTypeHouseState = FungibleHouseTokenState(valuationOfHouse,ourIdentity,UniqueIdentifier(),0,symbol)

        //warp it with transaction state specifying the notary
        val transactionState = evolvableTokenTypeHouseState withNotary notary!!

        //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
        val stx = subFlow(CreateEvolvableTokens(transactionState))

        return "Fungible house token $symbol has created with valuationL: $valuationOfHouse " +
                "\ntxId: ${stx.id}"
    }
}

