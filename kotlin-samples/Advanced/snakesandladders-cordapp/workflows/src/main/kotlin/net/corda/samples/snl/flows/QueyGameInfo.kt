package net.corda.samples.snl.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.services.AccountService
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.samples.snl.states.GameBoard
import java.util.*

@InitiatingFlow
@StartableByRPC
class QueyGameInfo(private val gameId: String) : FlowLogic<GameInfo>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): GameInfo {
        val linearStateQueryCriteria = LinearStateQueryCriteria(null, listOf(UUID.fromString(gameId)),
                null, StateStatus.UNCONSUMED, null)
        val gameBoardList = serviceHub.vaultService
                .queryBy(GameBoard::class.java, linearStateQueryCriteria).states
        if (gameBoardList.size == 0) throw FlowException("Game doesn't exist!")
        val gameBoard = gameBoardList[0].state.data
        val accountService: AccountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
        val player1 = accountService.accountInfo(gameBoard.player1.owningKey)!!.state.data.name
        val player2 = accountService.accountInfo(gameBoard.player2.owningKey)!!.state.data.name
        return GameInfo(
                gameBoard.linearId,
                player1,
                player2,
                gameBoard.currentPlayer,
                gameBoard.player1Pos,
                gameBoard.player2Pos,
                gameBoard.winner!!,
                gameBoard.lastRoll
        )
    }
}