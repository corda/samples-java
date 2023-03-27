package net.corda.samples.lending.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class SyndicateBidContract : Contract{

    companion object {
        @JvmStatic
        val ID = "net.corda.samples.lending.contracts.SyndicateBidContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[0]
        when(command.value){
            is Commands.Submit -> requireThat {
                /* At here, the syndication bid proposal from the syndication participating banks is verified.
                These contract rules make sure that each bid for syndicated loan is valid. */
            }
            is Commands.Approve -> requireThat {
                /* At here, the syndicated bid is verified for approval process. These contract rules make
                sure that all the conditions are met for the lead bank to approve the each syndicated bid. */
            }
        }
    }

    interface Commands : CommandData {
        class Submit : Commands
        class Approve : Commands
    }
}