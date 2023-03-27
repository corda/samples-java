package net.corda.samples.tictacthor.accountsUtilities


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.samples.tictacthor.states.BoardState
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class myGame(private val whoAmI:String) : FlowLogic<BoardState>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():BoardState {

        val myAccount = accountService.accountInfo(whoAmI).single().state.data
        val criteria = QueryCriteria.VaultQueryCriteria(
                externalIds = listOf(myAccount.identifier.id)
        )

        return serviceHub.vaultService.queryBy(
                contractStateType = BoardState::class.java,
                criteria = criteria
        ).states.single().state.data
    }
}
