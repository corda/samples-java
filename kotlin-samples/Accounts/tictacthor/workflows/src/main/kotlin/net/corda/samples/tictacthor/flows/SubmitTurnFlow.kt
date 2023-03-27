package net.corda.samples.tictacthor.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.samples.tictacthor.accountsUtilities.NewKeyForAccount
import net.corda.samples.tictacthor.contracts.BoardContract
import net.corda.samples.tictacthor.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.concurrent.atomic.AtomicReference

/*
This flow attempts submit a turn in the game.
It must be the initiating node's turn otherwise this will result in a FlowException.
*/

@InitiatingFlow
@StartableByRPC
class SubmitTurnFlow(private val gameId: UniqueIdentifier,
                     private val whoAmI: String,
                     private val whereTo:String,
                     private val x: Int,
                     private val y: Int) : FlowLogic<String>() {

    companion object {
        object GENERATING_KEYS : ProgressTracker.Step("Generating Keys for transactions.")
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction for between accounts")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.")
        object GATHERING_SIGS_FINISH : ProgressTracker.Step("Finish the counterparty's signature."){
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_KEYS,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION,
                GATHERING_SIGS_FINISH
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): String {

        //loading game board
        val myAccount = accountService.accountInfo(whoAmI).single().state.data
        val mykey = subFlow(NewKeyForAccount(myAccount.identifier.id)).owningKey

        val targetAccount = accountService.accountInfo(whereTo).single().state.data
        val targetAcctAnonymousParty = subFlow(RequestKeyForAccount(targetAccount))

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(gameId),
                Vault.StateStatus.UNCONSUMED, null)

        val inputBoardStateAndRef = serviceHub.vaultService.queryBy<BoardState>(queryCriteria)
                .states.singleOrNull()?: throw FlowException("GameState with id $gameId not found.")
        val inputBoardState = inputBoardStateAndRef.state.data

        // Check that the correct party executed this flow
        if (inputBoardState.getCurrentPlayerParty() != myAccount.identifier) throw FlowException("It's not your turn!")

        progressTracker.currentStep = GENERATING_TRANSACTION
        val command = Command(BoardContract.Commands.SubmitTurn(), listOf(mykey,targetAcctAnonymousParty.owningKey))
        val outputBoardState = inputBoardState.returnNewBoardAfterMove(Pair(x,y),AnonymousParty(mykey),targetAcctAnonymousParty)

        val notary = inputBoardStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(outputBoardState)
                .addCommand(command)
                .addInputState(inputBoardStateAndRef)
        //Pass along Transaction
        progressTracker.currentStep = SIGNING_TRANSACTION
        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder, listOf(ourIdentity.owningKey,mykey))


        //Collect sigs
        progressTracker.currentStep =GATHERING_SIGS
        val sessionForAccountToSendTo = initiateFlow(targetAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSendTo,
                targetAcctAnonymousParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        progressTracker.currentStep =FINALISING_TRANSACTION
        val stx = subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAccountToSendTo).filter { it.counterparty != ourIdentity }))
        subFlow(SyncGame(outputBoardState.linearId.toString(),targetAccount.host))
        return "rxId: ${stx.id}"
    }
}

@InitiatedBy(SubmitTurnFlow::class)
class SubmitTurnFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                // Custom Logic to validate transaction.
            }
        })
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
