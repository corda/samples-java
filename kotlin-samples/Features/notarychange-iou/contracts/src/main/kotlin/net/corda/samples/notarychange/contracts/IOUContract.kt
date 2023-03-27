package net.corda.samples.notarychange.contracts


import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.notarychange.states.IOUState


class IOUContract : Contract {
    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {

        val cmd = tx.commands.requireSingleCommand<Commands>()
        when (cmd.value) {
            is Commands.Create -> requireThat {
                val out: IOUState = tx.outputsOfType(IOUState::class.java)[0]
                // IOU-specific constraints.
                "The IOU's value must be non-negative.".using(out.value > 0)
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Settle : Commands
    }

    companion object {
        const val ID = "net.corda.samples.notarychange.contracts.IOUContract"
    }
}
