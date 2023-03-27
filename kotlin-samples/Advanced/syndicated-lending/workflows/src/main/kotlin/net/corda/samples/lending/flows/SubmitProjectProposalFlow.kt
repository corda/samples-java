package net.corda.samples.lending.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.lending.contracts.ProjectContract
import net.corda.samples.lending.states.ProjectState

@InitiatingFlow
@StartableByRPC
class SubmitProjectProposal(
        val lenders: List<Party>,
        val projectDescription: String,
        val projectCost: Int,
        val loanAmount: Int
        ): FlowLogic<SignedTransaction>() {

    @Override
    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val output = ProjectState(UniqueIdentifier(),projectDescription,ourIdentity,projectCost,loanAmount,lenders)

        val command = Command(ProjectContract.Commands.ProposeProject(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary)
                .addOutputState(output,ProjectContract.ID)
                .addCommand(command)

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val sessions = lenders.map { initiateFlow(it) }.toSet()

        return subFlow(FinalityFlow(ptx,sessions))
    }
}

@InitiatedBy(SubmitProjectProposal::class)
class SubmitProjectProposalResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}