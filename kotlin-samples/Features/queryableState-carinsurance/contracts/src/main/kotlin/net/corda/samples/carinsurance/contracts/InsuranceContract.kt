package net.corda.samples.carinsurance.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


// ************
// * Contract *
// ************
class InsuranceContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.carinsurance.contracts.InsuranceContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        val inputs = tx.inputs
        when (command.value) {
            is Commands.IssueInsurance -> requireThat {
                "Transaction must have no input states." using (inputs.isEmpty())
            }
            is Commands.AddClaim -> requireThat {
                "Insurance transaction must have input states, the insurance police" using (!inputs.isEmpty())
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class IssueInsurance : Commands
        class AddClaim : Commands

    }
}
