package net.corda.samples.lending.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StaticPointer
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.lending.contracts.LoanBidContract
import net.corda.samples.lending.states.LoanBidState
import net.corda.samples.lending.states.ProjectState

@InitiatingFlow
@StartableByRPC
class ApproveLoanBid (
        val bidIdentifier: UniqueIdentifier
        ) : FlowLogic<SignedTransaction>() {

    @Override
    @Suspendable
    override fun call(): SignedTransaction {

        val loanBidStateAndRefs = serviceHub.vaultService.queryBy(LoanBidState::class.java).states
        val inputStateAndRef = loanBidStateAndRefs.stream().filter {
            it.state.data.linearId.equals(bidIdentifier)
        }.findAny().orElseThrow { IllegalArgumentException("Loan Bid Not Found") }

        val inputState = inputStateAndRef.state.data

        val output = LoanBidState(
                projectDetails = StaticPointer(inputStateAndRef.ref, ProjectState::class.java),
                linearId = inputState.linearId, lender = inputState.lender,borrower = inputState.borrower,
                loanAmount = inputState.loanAmount,tenure = inputState.tenure, rateofInterest = inputState.rateofInterest,
                transactionFees = inputState.transactionFees, status = "APPROVED")

        val notary = inputStateAndRef.state.notary

        val builder = TransactionBuilder(notary)
                .addInputState(inputStateAndRef)
                .addOutputState(output)
                .addCommand(LoanBidContract.Commands.Approve(), listOf(inputState.borrower.owningKey,inputState.lender.owningKey))

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val session = initiateFlow(inputState.lender)
        val fullySignedTx = subFlow(
                CollectSignaturesFlow(ptx, listOf(session)))

        return subFlow(FinalityFlow(fullySignedTx, listOf(session)))
    }
}

@InitiatedBy(ApproveLoanBid::class)
class ApproveLoanBidResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
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