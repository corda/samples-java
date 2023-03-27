package net.corda.samples.logging.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.logging.states.YoState

// ************
// * Contract *
// ************
// Contract and state.
class YoContract : Contract {
    // Contract code.
    @Throws(IllegalArgumentException::class)
    // Contract code.
    override fun verify(tx: LedgerTransaction) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands.Send>()
        "There can be no inputs when Yo'ing other parties." using (tx.inputs.isEmpty())
        "There must be one output: The Yo!" using (tx.outputs.size == 1)
        val yo = tx.outputsOfType<YoState>().single()
        "No sending Yo's to yourself!" using (yo.target != yo.origin)
        "The Yo! must be signed by the sender." using (yo.origin.owningKey == command.signers.single())
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Send : Commands
    }

    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.logging.contracts.YoContract"
    }
}
