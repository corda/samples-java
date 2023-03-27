package net.corda.samples.supplychain.contracts


import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.supplychain.states.InvoiceState

class InvoiceStateContract : Contract{

    companion object{
        const val ID = "net.corda.samples.supplychain.contracts.InvoiceStateContract"
    }

    override fun verify(tx: LedgerTransaction) { /*
         * We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a signle transaction.
         */
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> requireThat {
                val inputs = tx.inputStates
                val outputs = tx.outputStates

                // no inputs to the transaction
                "No inputs should be consumed when creating a new SantaSession.".using(inputs.isEmpty())
                "Transaction must have exactly one output.".using(outputs.size == 1)

                val output = outputs[0] as InvoiceState
                // must be three or more players
                "Invoice amount must be a valid number (Greater than zero)".using(output.amount > 0)
                null
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}