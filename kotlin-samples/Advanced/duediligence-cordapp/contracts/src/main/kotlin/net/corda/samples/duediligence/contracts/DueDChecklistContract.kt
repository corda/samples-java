package net.corda.samples.duediligence.contracts

import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import java.lang.IllegalArgumentException

class DueDChecklistContract :Contract{

    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.duediligence.contracts.DueDChecklistContract"
    }

    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[0]

        //Propose request
        when (command.value){
            is Commands.Add -> requireThat {
                val (state) = tx.inputs[0]
                if (state.data.javaClass == CorporateRecordsAuditRequest::class.java) {
                    val request = state.data as CorporateRecordsAuditRequest
                    "Qualification Must be True".using(request.qualification)
                } else {
                    val request = state.data as CopyOfCoporateRecordsAuditRequest
                    "Qualification Must be True".using(request.qualification)
                }
            }
        }
    }

    interface Commands : CommandData {
        class Add : Commands
    }
}