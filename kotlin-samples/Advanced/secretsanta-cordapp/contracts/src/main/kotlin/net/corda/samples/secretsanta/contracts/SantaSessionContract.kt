package net.corda.samples.secretsanta.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.secretsanta.states.SantaSessionState

// ************
// * Contract *
// ************
class SantaSessionContract : Contract {

    companion object {
        @JvmStatic
        val ID = "net.corda.samples.secretsanta.contracts.SantaSessionContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        // In our secret santa app we only issue new Secret Santa game sessions shared with all players accounts
        class Issue : Commands
    }

    override fun verify(tx: LedgerTransaction) { /*
         * We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a signle transaction.
         */
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                val inputs = tx.inputStates
                val outputs = tx.outputStates

                // no inputs to the transaction
                "No inputs should be consumed when creating a new SantaSession.".using(inputs.isEmpty())
                "Transaction must have no input." using(inputs.size == 0)
                "Transaction must have exactly one output.".using(outputs.size == 1)
                "Output must be a SantaSessionState.".using(outputs[0] is SantaSessionState)
                // Retrieve the output state of the transaction
                val output = outputs[0] as SantaSessionState
                // must be three or more players
                "Must be three or more players".using(output.playerNames.size > 2)
                "Contact info for each player".using(output.playerEmails.size == output.playerNames.size)
                null
            }
        }
    }
}



