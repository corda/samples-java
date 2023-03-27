package net.corda.samples.snl.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.services.AccountService
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.sun.istack.NotNull
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.ReferencedStateAndRef
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.snl.contracts.GameBoardContract.Commands.PlayMove
import net.corda.samples.snl.oracle.flows.OracleSignatureFlow
import net.corda.samples.snl.states.BoardConfig
import net.corda.samples.snl.states.GameBoard
import java.security.SignatureException
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Predicate

class PlayerMoveFlow private constructor() {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val player: String, private val linearId: String, private val diceRolled: Int) : FlowLogic<SignedTransaction>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //notary
            val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

            val accountService: AccountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)

            //Get current player account info
            val currentPlayerAccountInfo = accountService.accountInfo(player)
            if (currentPlayerAccountInfo.size == 0) throw FlowException("Player $player doesn't exist!")
            val currPlayer: AbstractParty = subFlow(RequestKeyForAccount(currentPlayerAccountInfo[0].state.data))

            // Get game board
            val linearStateQueryCriteria = LinearStateQueryCriteria(null, listOf(UUID.fromString(linearId)),
                    null, StateStatus.UNCONSUMED, null)
            val gameBoardList = serviceHub.vaultService
                    .queryBy(GameBoard::class.java, linearStateQueryCriteria).states
            if (gameBoardList.size == 0) throw FlowException("Game doesn't exist!")
            val gameBoard = gameBoardList[0].state.data
            if (gameBoard.winner != null) {
                throw FlowException("This Game is Over")
            }

            // Check if the initiator is the current player
            if (gameBoard.currentPlayer != player) throw FlowException("Please wait for your turn")

            // Get the other player
            val otherPlayerAccountInfo = AtomicReference<AccountInfo>()
            gameBoard.participants.forEach(Consumer { abstractParty: AbstractParty ->
                val (name) = accountService.accountInfo(abstractParty.owningKey)!!.state.data
                if (name != player) {
                    otherPlayerAccountInfo.set(accountService.accountInfo(abstractParty.owningKey)!!.state.data)
                }
            })
            val otherPlayer: AbstractParty = subFlow(RequestKeyForAccount(otherPlayerAccountInfo.get()))
            val boardConfigList = serviceHub.vaultService.queryBy(BoardConfig::class.java).states
            if (boardConfigList.size == 0) throw FlowException("Board config missing")
            val boardConfig = boardConfigList[0].state.data

            // Calculate Player Position
            var newPlayer1Pos = gameBoard.player1Pos
            var newPlayer2Pos = gameBoard.player2Pos
            var winner: String? = null
            if (player == accountService.accountInfo(gameBoard.player1.owningKey)!!.state.data.name) {
                newPlayer1Pos = gameBoard.player1Pos + diceRolled
                if (newPlayer1Pos > 100) {
                    throw FlowException("You need to roll " + (100 - gameBoard.player1Pos) + " to win.")
                }
                if (boardConfig.ladderPositions!!.keys.contains(newPlayer1Pos)) newPlayer1Pos = boardConfig.ladderPositions!![newPlayer1Pos] ?: error("No Ladder Position") else if (boardConfig.snakePositions!!.keys.contains(newPlayer1Pos)) newPlayer1Pos = boardConfig.snakePositions!![newPlayer1Pos]?: error("No snake Position")
                if (newPlayer1Pos == 100) {
                    winner = accountService.accountInfo(gameBoard.player1.owningKey)!!.state.data.name
                }
            } else {
                newPlayer2Pos = gameBoard.player2Pos + diceRolled
                if (newPlayer2Pos > 100) {
                    throw FlowException("You need to roll " + (100 - gameBoard.player2Pos) + " to win.")
                }
                if (boardConfig.ladderPositions!!.keys.contains(newPlayer2Pos)) newPlayer2Pos = boardConfig.ladderPositions!![newPlayer2Pos]?: error("No Ladder Position") else if (boardConfig.snakePositions!!.keys.contains(newPlayer2Pos)) newPlayer2Pos = boardConfig.snakePositions!![newPlayer2Pos]?: error("No snake Position")
                if (newPlayer2Pos == 100) {
                    winner = accountService.accountInfo(gameBoard.player2.owningKey)!!.state.data.name
                }
            }
            val outputGameBoard = GameBoard(gameBoard.linearId,
                    gameBoard.player1, gameBoard.player2, otherPlayerAccountInfo.get().name,
                    newPlayer1Pos, newPlayer2Pos, winner, diceRolled)
            val transactionBuilder = TransactionBuilder(notary)
                    .addInputState(gameBoardList[0])
                    .addOutputState(outputGameBoard)
                    .addCommand(PlayMove(diceRolled), Arrays.asList(currPlayer.owningKey,
                            otherPlayer.owningKey))
                    .addReferenceState(ReferencedStateAndRef(boardConfigList[0]))
            transactionBuilder.verify(serviceHub)
            val selfSignedTransaction = serviceHub.signInitialTransaction(transactionBuilder, currPlayer.owningKey)
            val oracle = serviceHub.networkMapCache
                    .getNodeByLegalName(CordaX500Name.parse("O=Oracle,L=Mumbai,C=IN"))!!.legalIdentities[0]

            val ftx = selfSignedTransaction.buildFilteredTransaction(Predicate { o: Any? ->
                (o is Command<*> && (o as Command<*>).signers.contains(oracle.owningKey)
                        && (o as Command<*>).value is PlayMove)
            })

            val oracleSignature = subFlow(OracleSignatureFlow(oracle, ftx))!!
            val selfAndOracleSignedTransaction = selfSignedTransaction.withAdditionalSignature(oracleSignature)
            val otherPlayerSession = initiateFlow(otherPlayerAccountInfo.get().host)
            var signedTransaction = subFlow(CollectSignaturesFlow(selfAndOracleSignedTransaction, listOf(otherPlayerSession), listOf(currPlayer.owningKey)))
            if (!otherPlayerAccountInfo.get().host.equals(ourIdentity)) {
                signedTransaction = subFlow(FinalityFlow(signedTransaction, listOf(otherPlayerSession)))
                try {
                    accountService.shareStateAndSyncAccounts(signedTransaction
                            .toLedgerTransaction(serviceHub).outRef<ContractState>(0), otherPlayerSession.counterparty)
                } catch (e: SignatureException) {
                    e.printStackTrace()
                }
            } else {
                signedTransaction = subFlow(FinalityFlow(signedTransaction, emptyList()))
            }
            return signedTransaction
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction?>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction? {
            subFlow(object : SignTransactionFlow(counterpartySession) {
                @Throws(FlowException::class)
                override fun checkTransaction(@NotNull stx: SignedTransaction) {
                    // Custom Logic to validate transaction.
                }
            })
            return if (!counterpartySession.counterparty.equals(ourIdentity)) subFlow(ReceiveFinalityFlow(counterpartySession)) else null
        }
    }
}