package net.corda.samples.lending.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.lending.contracts.SyndicateContract
import net.corda.samples.lending.states.LoanBidState
import net.corda.samples.lending.states.ProjectState
import net.corda.samples.lending.states.SyndicateState

@InitiatingFlow
@StartableByRPC
class CreateSyndicate(
        val participantBanks: List<Party>,
        val projectIdentifier: UniqueIdentifier,
        val loanDetailIdentifier: UniqueIdentifier
) : FlowLogic<SignedTransaction>(){

    @Override
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val syndicateState = SyndicateState(
                UniqueIdentifier(), ourIdentity, participantBanks,
                LinearPointer(projectIdentifier, ProjectState::class.java),
                LinearPointer(loanDetailIdentifier, LoanBidState::class.java)
        )

        val builder = TransactionBuilder(notary)
                .addOutputState(syndicateState)
                .addCommand(SyndicateContract.Commands.Create(), syndicateState.leadBank.owningKey)

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val session = participantBanks.map { initiateFlow(it) }

        return subFlow(FinalityFlow(ptx, session))
    }
}

@InitiatedBy(CreateSyndicate::class)
class CreateSyndicateResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}