package net.corda.samples.lending.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StaticPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.lending.contracts.LoanBidContract
import net.corda.samples.lending.states.LoanBidState
import net.corda.samples.lending.states.ProjectState
import java.util.*

@InitiatingFlow
@StartableByRPC
class SubmitLoanBid(
        val borrower: Party,
        val loanAmount: Int,
        val tenure: Int,
        val ratioOfInterest: Double,
        val transactionFee: Int,
        val projectIdentifier: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Override
    @Suspendable
    override fun call(): SignedTransaction {

        val projectStateAndRefs = serviceHub.vaultService.queryBy(ProjectState::class.java).states
        val inputStateAndRef = projectStateAndRefs.stream().filter {
            it.state.data.linearId.equals(projectIdentifier)
        }.findAny().orElseThrow { IllegalArgumentException("Project Not Found") }

        val notary = inputStateAndRef.state.notary

        val output = LoanBidState(
                projectDetails = StaticPointer(inputStateAndRef.ref, ProjectState::class.java),
                linearId = UniqueIdentifier(), lender = ourIdentity,
                borrower = this.borrower, loanAmount = this.loanAmount,
                tenure = this.tenure, rateofInterest = this.ratioOfInterest,
                transactionFees = this.transactionFee, status = "SUBMITTED")


        val builder = TransactionBuilder(notary)
                .addOutputState(output)
                .addCommand(LoanBidContract.Commands.SubmitLoanBid(), listOf(ourIdentity.owningKey))

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val session = initiateFlow(borrower)

        return subFlow(FinalityFlow(ptx, listOf(session)))
    }
}


@InitiatedBy(SubmitLoanBid::class)
class SubmitLoanBidResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}