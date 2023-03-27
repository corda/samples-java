package net.corda.samples.lending.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.lending.states.LoanBidState

class LoanBidContract :Contract {

    companion object {
        @JvmStatic
        val ID = "net.corda.samples.lending.contracts.LoanBidContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[0]
        when(command.value){
            is Commands.SubmitLoanBid -> requireThat {
                /* At here, the loan bid proposal from the competing banks is verified.
                These contract rules make sure that each loan bid for project is valid. */
            }
            is Commands.Approve -> requireThat {
                /* At here, the loan bid is verified for approval process. These contract rules make
                sure that all the conditions are met for the borrower to approve the sole loan bid for its
                project. */
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class SubmitLoanBid : Commands
        class Approve : Commands
    }
}