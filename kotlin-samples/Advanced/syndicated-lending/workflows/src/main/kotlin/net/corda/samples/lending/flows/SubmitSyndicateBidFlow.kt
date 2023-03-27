package net.corda.samples.lending.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.lending.contracts.SyndicateBidContract
import net.corda.samples.lending.states.SyndicateBidState
import net.corda.samples.lending.states.SyndicateState

@InitiatingFlow
@StartableByRPC
class SubmitSyndicateBid(
        val syndicateIdentifier: UniqueIdentifier,
        val bidAmount: Int
) : FlowLogic<SignedTransaction>(){

    @Override
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val syndicateStateAndRefs = serviceHub.vaultService.queryBy(SyndicateState::class.java).states
        val syndicateStateAndRef = syndicateStateAndRefs.stream().filter {
            it.state.data.linearId.equals(syndicateIdentifier)
        }.findAny().orElseThrow { IllegalArgumentException("Syndicate Not Found") }

        val syndicateState = syndicateStateAndRef.state.data

        val syndicateBidState = SyndicateBidState(UniqueIdentifier(),LinearPointer(syndicateIdentifier, SyndicateState::class.java),
                bidAmount,syndicateState.leadBank,ourIdentity,"SUBMITTED")

        val builder = TransactionBuilder(notary)
                .addOutputState(syndicateBidState)
                .addCommand(SyndicateBidContract.Commands.Submit(), listOf(ourIdentity.owningKey))

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val session = initiateFlow(syndicateState.leadBank)

        return subFlow(FinalityFlow(ptx, listOf(session)))
    }
}

@InitiatedBy(SubmitSyndicateBid::class)
class SubmitSyndicateBidResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}