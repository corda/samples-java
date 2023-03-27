package net.corda.samples.tictacthor.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.samples.tictacthor.accountsUtilities.NewKeyForAccount
import net.corda.samples.tictacthor.contracts.BoardContract
import net.corda.samples.tictacthor.states.BoardState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.concurrent.atomic.AtomicReference

/*
This flow starts a game with another node by creating an new BoardState.
The responding node cannot decline the request to start a game.
The request is only denied if the responding node is already participating in a game.
*/

@InitiatingFlow
@StartableByRPC
class StartGameFlow(val whoAmI: String,
                    val whereTo: String) : FlowLogic<UniqueIdentifier>() {

    companion object {
        object GENERATING_KEYS : ProgressTracker.Step("Generating Keys for transactions.")
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction for between accounts")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
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
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): UniqueIdentifier {

        //Generate key for transaction
        progressTracker.currentStep = GENERATING_KEYS
        val myAccount = accountService.accountInfo(whoAmI).single().state.data
        val myKey = subFlow(NewKeyForAccount(myAccount.identifier.id)).owningKey

        val targetAccount = accountService.accountInfo(whereTo).single().state.data
        val targetAcctAnonymousParty = subFlow(RequestKeyForAccount(targetAccount))

        // If this node is already participating in an active game, decline the request to start a new one
        val criteria = QueryCriteria.VaultQueryCriteria(
                externalIds = listOf(myAccount.identifier.id)
        )
        val results = serviceHub.vaultService.queryBy(
                contractStateType = BoardState::class.java,
                criteria = criteria
        ).states

        progressTracker.currentStep = GENERATING_TRANSACTION

        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val command = Command(
                BoardContract.Commands.StartGame(),
                listOf(myKey,targetAcctAnonymousParty.owningKey))

        val initialBoardState = BoardState(
                myAccount.identifier,
                targetAccount.identifier,
                AnonymousParty(myKey),
                targetAcctAnonymousParty)
        val stateAndContract = StateAndContract(initialBoardState, BoardContract.ID)
        val txBuilder = TransactionBuilder(notary).withItems(stateAndContract, command)

        //Pass along Transaction
        progressTracker.currentStep = SIGNING_TRANSACTION
        txBuilder.verify(serviceHub)
        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder, listOfNotNull(ourIdentity.owningKey,myKey))

        //Collect sigs
        progressTracker.currentStep =GATHERING_SIGS
        val sessionForAccountToSendTo = initiateFlow(targetAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSendTo,
                listOf(targetAcctAnonymousParty.owningKey)))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)
        progressTracker.currentStep =FINALISING_TRANSACTION
        val stx = subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAccountToSendTo).filter { it.counterparty != ourIdentity }))
        return initialBoardState.linearId
    }
}


@InitiatedBy(StartGameFlow::class)
class StartGameFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(){
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                // Custom Logic to validate transaction.
            }
        })
        subFlow(ReceiveFinalityFlow(counterpartySession))
        }
}
