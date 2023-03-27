package net.corda.samples.tokenizedhouse.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState

// *********
// * Flows *
// *********
@StartableByRPC
class IssueHouseTokenFlow(val symbol: String,
                          val quantity: Long,
                          val holder:Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {

        //get house states on ledger with uuid as input tokenId
        val stateAndRef = serviceHub.vaultService.queryBy<FungibleHouseTokenState>()
                .states.filter { it.state.data.symbol.equals(symbol) }[0]

        //get the RealEstateEvolvableTokenType object
        val evolvableTokenType = stateAndRef.state.data

        //get the pointer pointer to the house
        val tokenPointer: TokenPointer<*> = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

        //assign the issuer to the house type who will be issuing the tokens
        val issuedTokenType = tokenPointer issuedBy ourIdentity

        //specify how much amount to issue to holder
        val amount = Amount(quantity,issuedTokenType)

        val fungibletoken = FungibleToken(amount,holder)

        val stx = subFlow(IssueTokens(listOf(fungibletoken)))
        return "Issued $quantity $symbol token to ${holder.name.organisation}"
    }
}
