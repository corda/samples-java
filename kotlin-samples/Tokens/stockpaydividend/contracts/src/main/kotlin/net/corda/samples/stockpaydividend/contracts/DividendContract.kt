package net.corda.samples.stockpaydividend.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.stockpaydividend.states.DividendState
import java.security.PublicKey

// ************
// * Contract *
// ************
class DividendContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.stockpaydividend.contracts.DividendContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command  = tx.commands.requireSingleCommand<DividendContract.Commands>()
        val requiredSigners = command.signers

        when(command.value){
            is Commands.Create -> requireThat {
                verifyCreate(tx,requiredSigners)
            }
            is Commands.Pay -> requireThat {
                verifyPay(tx,requiredSigners)
            }
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, requiredSigners: List<PublicKey>) {
        requireThat {
            // Add any validations that may fit
            val outputDividends: List<DividendState> = tx.outputsOfType(DividendState::class.java)
            "There must be one output dividend.".using(outputDividends.size == 1)
            val outputDividend = outputDividends[0]
            "Company and shareholder of the dividend should not be the same.".using(outputDividend.shareholder != outputDividend.company)
            "Both stock shareholder and company must sign the dividend receivable transaction.".using(requiredSigners.containsAll(keysFromParticipants(outputDividend)!!))
        }
    }

    private fun verifyPay(tx: LedgerTransaction, requiredSigners: List<PublicKey>) {
        requireThat{
            val inputDividends: List<DividendState> = tx.inputsOfType(DividendState::class.java)
            "There must be one input dividend.".using(inputDividends.size == 1)
            val outputDividends: List<DividendState> = tx.outputsOfType(DividendState::class.java)
            "There should be no output dividends.".using(outputDividends.isEmpty())
            val inputDividend = inputDividends[0]
            "Both stock shareholder and company must sign the dividend receivable transaction.".using(requiredSigners.containsAll(keysFromParticipants(inputDividend)!!))
        }
    }


    private fun keysFromParticipants(dividend: DividendState): Set<PublicKey?>? {
        return dividend.participants.map { it.owningKey }.toSet()
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create : Commands
        class Pay : Commands
    }
}