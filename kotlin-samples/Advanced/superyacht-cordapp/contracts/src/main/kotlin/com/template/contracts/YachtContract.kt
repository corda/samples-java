package com.template.contracts

import com.template.states.YachtState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat
// ************
// * Contract *
// ************
class YachtContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.YachtContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        val inputs = tx.inputStates
        val outputs = tx.outputStates
        val firstOutput = tx.outputsOfType<YachtState>()[0]

        when (command.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed when creating a new YachtState.".using(inputs.isEmpty())
                "There should only be one output when creating a new YachtState.".using(outputs.size == 1)
                "The owner must be required signers".using(command.signers.contains(firstOutput.owner.owningKey))
            }
            is Commands.Purchase -> requireThat{
                val firstInput = tx.inputsOfType<YachtState>()[0]
                "There should be two input when purchasing a Yacht.".using(inputs.size == 2)
                "The yacht must be marked as for sale.".using(firstInput.forSale)
                "The seller and the buyer must be required signers.".using(command.signers.containsAll(firstInput.participants.map {it.owningKey}))
                "The seller and the buyer cannot be the same entity.".using(firstInput.owner != firstOutput.owner)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
        class Purchase : Commands
    }
}