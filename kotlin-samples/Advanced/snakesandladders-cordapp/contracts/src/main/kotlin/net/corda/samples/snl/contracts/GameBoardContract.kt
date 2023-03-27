package net.corda.samples.snl.contracts


import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.snl.states.BoardConfig
import net.corda.samples.snl.states.GameBoard


class GameBoardContract : Contract {

    companion object {
        var ID = "net.corda.samples.snl.contracts.GameBoardContract"
    }

    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        // Contract verification logic should be implemented here
        if (tx.commands.size != 1) throw IllegalArgumentException("One command Expected")
        if (tx.getCommand<CommandData>(0).value is Commands.Create) {
            verifyCreate(tx)
        } else if (tx.getCommand<CommandData>(0).value is Commands.PlayMove) {
            verifyPlay(tx)
        }
    }

    @Throws(IllegalArgumentException::class)
    private fun verifyCreate(tx: LedgerTransaction) {
        // Contract verification logic for create game should be implemented here
        if (tx.outputStates.size != 1 || tx.inputStates.size != 0) throw IllegalArgumentException("Zero Input and One Output Expected")
        if (tx.getOutput(0) !is GameBoard) throw IllegalArgumentException("Output of type GameBoard expected")
    }

    @Throws(IllegalArgumentException::class)
    private fun verifyPlay(tx: LedgerTransaction) {
        // Contract verification logic for play move should be implemented here
        if (tx.references.size == 0 || tx.getReferenceInput(0) !is BoardConfig) {
            throw IllegalArgumentException("One reference Input of BoardConfig Expected")
        }
        if (tx.outputStates.size != 1 || tx.inputStates.size != 1) throw IllegalArgumentException("One Input and One Output Expected")
    }

    interface Commands : CommandData {
        class Create : Commands
        class PlayMove(val roll: Int) : Commands
    }
}
