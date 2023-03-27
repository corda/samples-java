package com.tutorial.flows

import co.paralleluniverse.fibers.Suspendable
import com.tutorial.contracts.BasketOfApplesContract.Commands.packBasket
import com.tutorial.states.BasketOfApples
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class PackApplesInitiator(private val appleDescription: String, private val weight: Int) : FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {

        /* Obtain a reference to a notary we wish to use.
         * METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
         *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)
         *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
         */
        val notary = serviceHub.networkMapCache.notaryIdentities[0] // METHOD 1
        //final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        //Create the output object
        val basket = BasketOfApples(description = appleDescription, farm = ourIdentity, weight = weight)

        //Building transaction
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(basket)
                .addCommand(packBasket(), ourIdentity.owningKey)

        // Verify the transaction
        txBuilder.verify(serviceHub)

        // Sign the transaction
        val signedTransaction = serviceHub.signInitialTransaction(txBuilder)

        // Notarise the transaction and record the states in the ledger.
        return subFlow(FinalityFlow(signedTransaction, emptyList()))
    }
}
