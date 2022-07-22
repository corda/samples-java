package com.tutorial.flows

import co.paralleluniverse.fibers.Suspendable
import com.tutorial.contracts.BasketOfApplesContract
import com.tutorial.states.AppleStamp
import com.tutorial.states.BasketOfApples
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault.RelevancyStatus
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.security.PublicKey
import java.util.*

@InitiatingFlow
@StartableByRPC
class RedeemApplesInitiator(private val buyer: Party, private val stampId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        /* Obtain a reference to a notary we wish to use.
         * METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        val notary = serviceHub.networkMapCache.notaryIdentities[0] // METHOD 1
        //final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        //Query the AppleStamp
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria()
                .withUuid(listOf(UUID.fromString(stampId.toString())))
                .withStatus(StateStatus.UNCONSUMED)
                .withRelevancyStatus(RelevancyStatus.RELEVANT)
        val appleStampStateAndRef: StateAndRef<*> = serviceHub.vaultService.queryBy(AppleStamp::class.java, inputCriteria).states.get(0)

        //Query output BasketOfApples
        val outputCriteria: QueryCriteria = QueryCriteria.VaultQueryCriteria()
                .withStatus(StateStatus.UNCONSUMED)
                .withRelevancyStatus(RelevancyStatus.RELEVANT)
        val BasketOfApplesStateAndRef: StateAndRef<*> = serviceHub.vaultService.queryBy(BasketOfApples::class.java, outputCriteria).states[0]
        val originalBasketOfApples = BasketOfApplesStateAndRef.state.data as BasketOfApples

        //Modify output to address the owner change
        val output = originalBasketOfApples.changeOwner(buyer)

        //Build Transaction
        val txBuilder = TransactionBuilder(notary)
                .addInputState(appleStampStateAndRef)
                .addInputState(BasketOfApplesStateAndRef)
                .addOutputState(output, BasketOfApplesContract.ID)
                .addCommand(BasketOfApplesContract.Commands.Redeem(),
                        Arrays.asList(ourIdentity.owningKey, buyer.owningKey))

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(buyer)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession)))

        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)))
    }
}

@InitiatedBy(RedeemApplesInitiator::class)
class RedeemApplesResponder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val signedTransaction = subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        })

        //Stored the transaction into data base.
        return subFlow(ReceiveFinalityFlow(counterpartySession, signedTransaction.id))
    }
}
