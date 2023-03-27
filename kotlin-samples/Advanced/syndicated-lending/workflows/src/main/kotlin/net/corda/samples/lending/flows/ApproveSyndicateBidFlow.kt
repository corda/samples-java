package net.corda.samples.lending.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.lending.contracts.SyndicateBidContract
import net.corda.samples.lending.states.SyndicateBidState

@InitiatingFlow
@StartableByRPC
class ApproveSyndicateBid(
        val bidIdentifier: UniqueIdentifier
): FlowLogic<SignedTransaction>() {

    @Override
    @Suspendable
    override fun call(): SignedTransaction {

        val syndicateBidStateAndRefs = serviceHub.vaultService.queryBy(SyndicateBidState::class.java).states
        val syndicateBidStateAndRef = syndicateBidStateAndRefs.stream().filter {
            it.state.data.linearId.equals(bidIdentifier)
        }.findAny().orElseThrow { IllegalArgumentException("Syndicate Bid Not Found") }

        val inputState: SyndicateBidState = syndicateBidStateAndRef.state.data

        val outputState = SyndicateBidState(inputState.linearId,inputState.syndicateState,
                inputState.bidAmount,inputState.leadBank,inputState.participateBank,"APPROVED")

        val builder = TransactionBuilder(syndicateBidStateAndRef.state.notary)
                .addInputState(syndicateBidStateAndRef)
                .addOutputState(outputState)
                .addCommand(SyndicateBidContract.Commands.Approve(), listOf(ourIdentity.owningKey,inputState.participateBank.owningKey))

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val session = initiateFlow(inputState.participateBank)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(ptx, listOf(session)))

        return subFlow(FinalityFlow(fullySignedTx, listOf(session)))
    }


}

@InitiatedBy(ApproveSyndicateBid::class)
class ApproveSyndicateBidResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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