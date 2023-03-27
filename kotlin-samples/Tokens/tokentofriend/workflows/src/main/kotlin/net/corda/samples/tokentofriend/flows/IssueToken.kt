package net.corda.samples.tokentofriend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.samples.tokentofriend.states.CustomTokenState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AbstractParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker
import java.util.*

// *********
// * Flows *
// *********
@StartableByRPC
class IssueToken(val uuid: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        /* Get a reference of own identity */
        val issuer = ourIdentity

        /* Fetch the house state from the vault using the vault query */
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(UUID.fromString(uuid)),status = Vault.StateStatus.UNCONSUMED)
        val customTokenState = serviceHub.vaultService.queryBy(CustomTokenState::class.java, criteria = inputCriteria).states.single().state.data

        /*
        * Create an instance of IssuedTokenType, it is used by our Non-Fungible token which would be issued to the owner. Note that the IssuedTokenType takes
        * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState. This is done to separate the state info from the token
        * so that the state can evolve independently.
        * IssuedTokenType is a wrapper around the TokenType and the issuer.
        * */
        val issuedToken = customTokenState.toPointer(customTokenState.javaClass) issuedBy  issuer

        /* Create an instance of the non-fungible house token with the owner as the token holder. The last paramter is a hash of the jar containing the TokenType, use the helper function to fetch it. */
        val storageNode = storageSelector()
        val tokenId = UniqueIdentifier()
        val token = NonFungibleToken(issuedToken, storageNode, tokenId)

        /* Issue the house token by calling the IssueTokens flow provided with the TokenSDK */
        val stx = subFlow(IssueTokens(listOf(token)))

        return "\nMessage: "+ customTokenState.message + "\nToken Id is: "+tokenId+"\nStorage Node is: "+storageNode;
    }

    fun storageSelector():AbstractParty{
        val notary = serviceHub.networkMapCache.notaryIdentities.single() // METHOD 1
        val allOtherNodes = serviceHub.networkMapCache.allNodes.filter {
            it.legalIdentities.single() != ourIdentity && it.legalIdentities.single() != notary}
        val pick = (0..allOtherNodes.size*10).shuffled().first()/10
        return allOtherNodes.get(pick).legalIdentities.single()
    }
}


