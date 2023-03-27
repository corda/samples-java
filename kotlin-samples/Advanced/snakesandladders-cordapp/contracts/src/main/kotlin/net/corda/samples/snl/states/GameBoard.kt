package net.corda.samples.snl.states


import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.samples.snl.contracts.GameBoardContract
import java.util.*


@BelongsToContract(GameBoardContract::class)
class GameBoard(override val linearId: UniqueIdentifier, val player1: AbstractParty, val player2: AbstractParty,
                val currentPlayer: String, val player1Pos: Int, val player2Pos: Int, val winner: String?, val lastRoll: Int) : LinearState {
    override val participants: List<AbstractParty>
        get() = Arrays.asList(player1, player2)

}
