package net.corda.samples.tokentofriend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.samples.tokentofriend.states.CustomTokenState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker
import java.util.*

// *********
// * Flows *
// *********
@StartableByRPC
class QueryToken(val uuid: String, private val recipientEmail: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {

        val receivedToken = try {
            val inputCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(UUID.fromString(uuid)),status = Vault.StateStatus.UNCONSUMED)
            serviceHub.vaultService.queryBy(NonFungibleToken::class.java, criteria = inputCriteria).states.single().state.data
        }catch (e:java.util.NoSuchElementException){
            return "\nERROR: Your Token ID Cannot Be Found In The System"
        }
        val tokenTypeStateId = receivedToken.token.tokenIdentifier

        val underlineState = try {
            val inputCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(UUID.fromString(tokenTypeStateId)),status = Vault.StateStatus.UNCONSUMED)
            serviceHub.vaultService.queryBy(CustomTokenState::class.java, criteria = inputCriteria).states.single().state.data
        }catch (e:java.util.NoSuchElementException){
            return "\nERROR: Internal Error"
        }
         if (underlineState.recipient == recipientEmail){
             return "\nCreator of the Token: " + underlineState.issuer +
                     "\nMessage: " + underlineState.message
         }else{
             return "\nToken found, but the recipient email is not matched. Please try again"
         }
    }
}