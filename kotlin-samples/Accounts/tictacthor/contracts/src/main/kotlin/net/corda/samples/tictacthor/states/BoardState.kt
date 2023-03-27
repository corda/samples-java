package net.corda.samples.tictacthor.states

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.samples.tictacthor.contracts.BoardContract
import java.security.PublicKey
import java.util.*
import kotlin.IllegalStateException

@CordaSerializable
enum class Status {
    GAME_IN_PROGRESS, GAME_OVER
}

@BelongsToContract(BoardContract::class)
@CordaSerializable
data class BoardState(val playerO: UniqueIdentifier,
                      val playerX: UniqueIdentifier,
                      val me: AnonymousParty,
                      val competitor:AnonymousParty,
                      val isPlayerXTurn: java.lang.Boolean = java.lang.Boolean(false),
                      val board: Array<CharArray> = Array(3, {charArrayOf('E', 'E', 'E')} ),
                      val status: Status = Status.GAME_IN_PROGRESS,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {

    override val participants: List<AbstractParty> = listOfNotNull(me,competitor).map {it}

    // Returns the party of the current player
    fun getCurrentPlayerParty(): UniqueIdentifier { return if (isPlayerXTurn.booleanValue()) playerX else playerO }
    // Get deep copy of board
    private fun Array<CharArray>.copy() = Array(size) { get(it).clone() }

    // Returns a copy of a BoardState object after a move at Pair<x,y>
    fun returnNewBoardAfterMove(pos: Pair<Int,Int>, me:AnonymousParty, competitor:AnonymousParty): BoardState {
        if (pos.first > 2 || pos.second > 2) throw IllegalStateException("Invalid board index.")
        val newBoard = board.copy()
        if (isPlayerXTurn.booleanValue()) newBoard[pos.second][pos.first] = 'X'
        else newBoard[pos.second][pos.first] = 'O'

        val newBoardState = copy(board = newBoard,
                isPlayerXTurn = java.lang.Boolean(!isPlayerXTurn.booleanValue()),
                me = me,
                competitor = competitor)
        if (BoardContract.BoardUtils.isGameOver(newBoardState)) return newBoardState.copy(status = Status.GAME_OVER)
        return newBoardState
    }
}

