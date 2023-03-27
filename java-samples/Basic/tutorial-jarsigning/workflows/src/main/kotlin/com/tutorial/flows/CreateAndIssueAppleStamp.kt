package com.tutorial.flows

import co.paralleluniverse.fibers.Suspendable
import com.tutorial.contracts.AppleStampContract
import com.tutorial.states.AppleStamp
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateAndIssueAppleStampInitiator(private val stampDescription: String, private val holder: Party) : FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {

        /* Obtain a reference to a notary we wish to use.
        *  METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
        *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flows or parsed from config (Preferred)
        *  * - For production you always want to1 use Method 2 as it guarantees the expected notary is returned.
        */
        val notary = serviceHub.networkMapCache.notaryIdentities[0] // METHOD 1
        //final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

        //Building the output AppleStamp state
        val uniqueID = UniqueIdentifier()
        val newStamp = AppleStamp(stampDescription, ourIdentity, this.holder, uniqueID)

        //Compositing the transaction
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(newStamp)
                .addCommand(AppleStampContract.Commands.Issue(),
                        listOf(ourIdentity.owningKey, holder.owningKey))

        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Send the state to the counterparty, and receive it back with their signature.

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(holder)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(partSignedTx, Arrays.asList(otherPartySession)))

        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, Arrays.asList(otherPartySession)))
    }
}

@InitiatedBy(CreateAndIssueAppleStampInitiator::class)
class CreateAndIssueAppleStampResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                //Addition checks
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

