package net.corda.samples.snl.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.snl.states.BoardConfig


class BoardConfigContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        // Contract verification logic should be implemented here
        if (tx.outputStates.size != 1 || tx.inputStates.size != 0) throw IllegalArgumentException("Zero Input and One Output Expected")
        if (tx.getOutput(0) !is BoardConfig) throw IllegalArgumentException("Output of type BoardConfig expected")
        val boardConfig = tx.getOutput(0) as BoardConfig
        if (boardConfig.snakePositions == null || boardConfig.snakePositions.size == 0 || boardConfig.ladderPositions == null || boardConfig.ladderPositions.size == 0) throw IllegalArgumentException("Snake and Ladder Positions should not be empty or null")
    }

    interface Commands : CommandData {
        class Create : GameBoardContract.Commands
    }

    companion object {
        var ID = "net.corda.samples.snl.contracts.BoardConfigContract"
    }
}
