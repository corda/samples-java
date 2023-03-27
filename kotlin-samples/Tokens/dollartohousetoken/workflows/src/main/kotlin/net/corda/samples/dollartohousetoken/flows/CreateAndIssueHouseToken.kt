package net.corda.samples.dollartohousetoken.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.dollartohousetoken.states.HouseState
import java.util.*

// *********
// * Flows *
// *********
@StartableByRPC
class CreateAndIssueHouseToken(val owner: Party,
                               val valuationOfHouse: Amount<Currency>,
                               val noOfBedRooms: Int,
                               val constructionArea: String,
                               val additionInfo: String,
                               val address: String
                               ) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        /* Get a reference of own identity */
        val issuer = ourIdentity

        /* Construct the output state */
        val uuid = fromString(UUID.randomUUID().toString())
        val houseState = HouseState(UniqueIdentifier(),Arrays.asList(issuer),valuationOfHouse,noOfBedRooms,constructionArea,additionInfo,address)

        /* Create an instance of TransactionState using the houseState token and the notary */
        val transactionState = houseState withNotary notary!!

        /* Create the house token. TokenSDK provides the CreateEvolvableTokens flow which could be called to create an evolvable token in the ledger.*/
        subFlow(CreateEvolvableTokens(transactionState))

        /*
        * Create an instance of IssuedTokenType, it is used by our Non-Fungible token which would be issued to the owner. Note that the IssuedTokenType takes
        * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState. This is done to separate the state info from the token
        * so that the state can evolve independently.
        * IssuedTokenType is a wrapper around the TokenType and the issuer.
        * */

        val issuedHouseToken = houseState.toPointer(houseState.javaClass) issuedBy  issuer

        /* Create an instance of the non-fungible house token with the owner as the token holder. The last paramter is a hash of the jar containing the TokenType, use the helper function to fetch it. */
        val houseToken = NonFungibleToken(issuedHouseToken, owner, UniqueIdentifier())

        /* Issue the house token by calling the IssueTokens flow provided with the TokenSDK */
        val stx = subFlow(IssueTokens(listOf(houseToken)))

        return ("\nThe non-fungible house token is created with UUID: " + houseState.linearId + ". (This is what you will use in next step)"
                + "\nTransaction ID: " + stx.id)
    }
}
