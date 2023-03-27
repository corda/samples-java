package net.corda.samples.tictacthor.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.samples.tictacthor.accountsUtilities.NewKeyForAccount
import net.corda.samples.tictacthor.contracts.BoardContract
import net.corda.samples.tictacthor.states.BoardState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/*
This flow ends a game by removing the BoardState from the ledger.
This flow is started through an request from the frontend once the GAME_OVER status is detected on the BoardState.
*/

@InitiatingFlow
@StartableByRPC
class EndGameFlow(private val gameId: UniqueIdentifier,
                  private val whoAmI: String,
                  private val whereTo:String) : FlowLogic<SignedTransaction>() {

    // TODO: progressTracker
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        //loading game board
        val myAccount = accountService.accountInfo(whoAmI).single().state.data
        val mykey = subFlow(NewKeyForAccount(myAccount.identifier.id)).owningKey

        val targetAccount = accountService.accountInfo(whereTo).single().state.data
        val targetAcctAnonymousParty = subFlow(RequestKeyForAccount(targetAccount))


        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(gameId),
                Vault.StateStatus.UNCONSUMED, null)
        val boardStateRefToEnd = serviceHub.vaultService.queryBy<BoardState>(queryCriteria)
                .states.singleOrNull()?:throw FlowException("GameState with id $gameId not found.")

        val command = Command(BoardContract.Commands.EndGame(),listOf(mykey,targetAcctAnonymousParty.owningKey))

        val notary = boardStateRefToEnd.state.notary
        val txBuilder = TransactionBuilder(notary)
                .addInputState(boardStateRefToEnd)
                .addCommand(command)
        txBuilder.verify(serviceHub)

        //self sign
        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder, listOf(ourIdentity.owningKey,mykey))
        //counter sign
        val sessionForAccountToSendTo = initiateFlow(targetAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSendTo,
                targetAcctAnonymousParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        return subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAccountToSendTo).filter { it.counterparty != ourIdentity }))
    }
}

@InitiatedBy(EndGameFlow::class)
class EndGameFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {}
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
