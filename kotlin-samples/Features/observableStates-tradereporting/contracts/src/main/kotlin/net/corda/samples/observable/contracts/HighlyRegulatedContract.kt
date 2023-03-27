package net.corda.samples.observable.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.observable.states.HighlyRegulatedState

class HighlyRegulatedContract : Contract {
    companion object {
        const val ID = "net.corda.samples.observable.contracts.HighlyRegulatedContract"
    }

    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val cmd = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputsOfType(HighlyRegulatedState::class.java)[0]
        when(cmd.value){
            is Commands.Trade -> requireThat {
                "The Buyer and the seller cannot be the same entity." using (!output.buyer.equals(output.seller))
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Trade : Commands
    }
}