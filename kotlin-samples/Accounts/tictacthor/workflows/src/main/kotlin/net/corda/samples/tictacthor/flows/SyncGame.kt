package net.corda.samples.tictacthor.flows


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts
import net.corda.samples.tictacthor.states.BoardState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class SyncGame(private val gameId: String,
               private val party: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {

        val id = UniqueIdentifier.fromString(gameId)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(id),
                Vault.StateStatus.UNCONSUMED, null)
        val inputBoardStateAndRef = serviceHub.vaultService.queryBy<BoardState>(queryCriteria).states.singleOrNull()?:
        throw FlowException("GameState with id $gameId not found.")
        subFlow(ShareStateAndSyncAccounts(inputBoardStateAndRef,party))
        return "Game synced"
    }
}
