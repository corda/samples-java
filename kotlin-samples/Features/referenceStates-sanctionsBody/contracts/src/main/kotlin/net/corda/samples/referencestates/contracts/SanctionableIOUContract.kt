package net.corda.samples.referencestates.contracts

import net.corda.samples.referencestates.states.SanctionableIOUState
import net.corda.samples.referencestates.states.SanctionedEntities
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contracts in Corda.
 *
 * This contracts enforces rules regarding the creation of a valid [SanctionableIOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output states: the new [IOU].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
class SanctionableIOUContract : Contract {
    companion object {
        @JvmStatic
        val IOU_CONTRACT_ID = "net.corda.samples.referencestates.contracts.SanctionableIOUContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()


        require(tx.referenceInputRefsOfType(SanctionedEntities::class.java).singleOrNull() != null) {
            "All transactions require a list of sanctioned entities"
        }

        val sanctionedEntities = tx.referenceInputRefsOfType(SanctionedEntities::class.java).single().state.data
        require(sanctionedEntities.issuer.name == command.value.sanctionsBody.name) {
            "${sanctionedEntities.issuer.name.organisation} is an invalid issuer of sanctions lists for this contracts"
        }

        requireThat {
            // Generic constraints around the IOU transaction.
            "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
            "Only one output states should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<SanctionableIOUState>().single()
            "The lender and the borrower cannot be the same entity." using (out.lender != out.borrower)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // IOU-specific constraints.
            "The IOU's value must be non-negative." using (out.value > 0)

            // IOU cannot involve a sanctioned entity
            "The lender ${out.lender.name} is a sanctioned entity" using !sanctionedEntities.badPeople.contains(out.lender)
            "The borrower ${out.borrower.name} is a sanctioned entity" using !sanctionedEntities.badPeople.contains(out.borrower)
        }
    }

    /**
     * This contracts only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create(val sanctionsBody: Party) : Commands
    }
}
