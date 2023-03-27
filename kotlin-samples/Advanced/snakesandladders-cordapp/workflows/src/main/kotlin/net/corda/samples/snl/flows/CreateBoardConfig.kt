package net.corda.samples.snl.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.commands.Create
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.services.AccountService
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
import com.sun.istack.NotNull
import net.corda.core.contracts.ContractState
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.snl.states.BoardConfig
import java.security.SignatureException
import java.util.*

class CreateBoardConfig private constructor() {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val player1: String, private val player2: String) : FlowLogic<SignedTransaction>() {
        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //notary
            val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
            val accountService: AccountService = serviceHub.cordaService(KeyManagementBackedAccountService::class.java)
            val p1accountInfo = accountService.accountInfo(player1)
            if (p1accountInfo.size == 0) throw FlowException("Player $player1 doesn't exist!")
            val p2accountInfo = accountService.accountInfo(player2)
            if (p1accountInfo.size == 0) throw FlowException("Player $player2 doesn't exist!")
            val player1: AbstractParty = subFlow(RequestKeyForAccount(p1accountInfo[0].state.data))
            val player2: AbstractParty = subFlow(RequestKeyForAccount(p2accountInfo[0].state.data))
            val ladderPositions: MutableMap<Int, Int> = LinkedHashMap()
            ladderPositions[2] = 45
            ladderPositions[4] = 27
            ladderPositions[9] = 31
            ladderPositions[47] = 84
            ladderPositions[70] = 87
            ladderPositions[71] = 91
            val snakePositions: MutableMap<Int, Int> = LinkedHashMap()
            snakePositions[16] = 8
            snakePositions[52] = 28
            snakePositions[78] = 25
            snakePositions[93] = 89
            snakePositions[95] = 75
            snakePositions[99] = 21
            val boardConfig = BoardConfig(ladderPositions, snakePositions, Arrays.asList(player1, player2))
            val transactionBuilder = TransactionBuilder(notary)
                    .addOutputState(boardConfig)
                    .addCommand(Create(), Arrays.asList(player1.owningKey, player2.owningKey))
            transactionBuilder.verify(serviceHub)
            val selfSignedTransaction = serviceHub.signInitialTransaction(transactionBuilder, player1.owningKey)
            val player2Session = initiateFlow(p2accountInfo[0].state.data.host)
            var signedTransaction = subFlow(CollectSignaturesFlow(selfSignedTransaction, listOf(player2Session), setOf(player1.owningKey)))
            return if (!p2accountInfo[0].state.data.host.equals(ourIdentity)) {
                signedTransaction = subFlow(FinalityFlow(signedTransaction, listOf(player2Session)))
                try {
                    accountService.shareStateAndSyncAccounts(signedTransaction
                            .toLedgerTransaction(serviceHub).outRef<ContractState>(0), player2Session.counterparty)
                } catch (e: SignatureException) {
                    e.printStackTrace()
                }
                signedTransaction
            } else {
                subFlow(FinalityFlow(signedTransaction, emptyList()))
            }
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