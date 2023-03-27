package net.corda.samples.statereissuance.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.statereissuance.states.LandTitleState

class LandTitleContract : Contract {

    override fun verify(tx: LedgerTransaction) {
        if (tx.commands.isEmpty()) {
            throw java.lang.IllegalArgumentException("One command Expected")
        }

        val command = tx.getCommand<CommandData>(0)

        when (command.value) {
            is Commands.Issue -> {
                verifyIssue(tx)
            }
            is Commands.Transfer -> {
                verifyTransfer(tx)
            }
            is Commands.Exit -> {
                verifyExit(tx)
            }
        }
    }

    private fun verifyIssue(tx: LedgerTransaction) {
        // Land Title Issue Contract Verification Logic goes here
        if (tx.outputStates.size != 1) throw IllegalArgumentException("One Output Expected")
        val command = tx.getCommand<CommandData>(0)
        if (!command.signers.contains((tx.getOutput(0) as LandTitleState).issuer.owningKey))
            throw IllegalArgumentException("Issuer Signature Required")
    }

    private fun verifyTransfer(tx: LedgerTransaction) {
        // Land Title Transfer Contract Verification Logic goes here
        val command = tx.getCommand<CommandData>(0)
        val landTitleState = tx.getInput(0) as LandTitleState
        if (!command.signers.contains(landTitleState.issuer.owningKey) && (landTitleState.owner != null && command.signers.contains(landTitleState.owner.owningKey)))
            throw java.lang.IllegalArgumentException("Issuer and Owner must Sign")
    }

    private fun verifyExit(tx: LedgerTransaction) {
        // Land Title Exit Contract Verification Logic goes here
        if (tx.outputStates.size != 0) throw java.lang.IllegalArgumentException("Zero Output Expected")
        if (tx.inputStates.size != 1) throw java.lang.IllegalArgumentException("One Input Expected")
        val command = tx.getCommand<CommandData>(0)
        if (!command.signers.contains((tx.getInput(0) as LandTitleState).issuer.owningKey)) throw java.lang.IllegalArgumentException("Issuer Signature Required")
    }

    interface Commands : CommandData {
        class Issue : Commands
        class Transfer : Commands
        class Exit : Commands
        class Reissue : Commands
    }
}