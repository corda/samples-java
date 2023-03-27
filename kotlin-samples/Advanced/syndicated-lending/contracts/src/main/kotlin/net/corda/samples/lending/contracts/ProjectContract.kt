package net.corda.samples.lending.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class ProjectContract : Contract{
    companion object {
        @JvmStatic
        val ID = "net.corda.samples.lending.contracts.ProjectContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[0]
        when(command.value){
            is Commands.ProposeProject -> requireThat {
                /*At here, you can structure the rules for creating a project proposal
                * this verify method makes sure that all proposed projects from the borrower company
                * are sound, so that banks are not going to waste any time on unqualified project proposals*/
            }
        }
    }

    interface Commands : CommandData {
        class ProposeProject : Commands
    }
}