package net.corda.samples.lending.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SyndicateContract : Contract{

    companion object {
        @JvmStatic
        val ID = "net.corda.samples.lending.contracts.SyndicateContract"
    }


    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[0]
        when(command.value){
            is Commands.Create -> requireThat {
                /*Here writes the rules for the lead bank's creating the syndication.*/
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
    }
}