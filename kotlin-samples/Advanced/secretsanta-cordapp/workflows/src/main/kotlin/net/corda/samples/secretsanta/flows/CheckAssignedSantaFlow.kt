package net.corda.samples.secretsanta.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.samples.secretsanta.states.SantaSessionState
import java.util.*


/**
 * This flow will create the account on the node on which you run this flow. This is done using inbuilt flow called CreateAccount.
 * CreateAccount creates an AccountInfo object which has name, host and id as its fields. This is mapped to Account table in the db.
 * For any other party to transact with this account, this AccountInfo will have to be shared with that Party.
 * Hence the Ipl Ticket Dealers create the ticket buyers accounts on their end and share this accountInfo with the Bank node and BCCI node.
 */
@StartableByRPC
@InitiatingFlow
class CheckAssignedSantaFlow(val santaSessionId: UniqueIdentifier) : FlowLogic<SantaSessionState>() {
    var santaSessionState: SantaSessionState? = null
        private set

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SantaSessionState {

        // Retrieve the Game State from the vault using LinearStateQueryCriteria
        val listOfLinearIds = Arrays.asList(santaSessionId.id)
        val queryCriteria: QueryCriteria = LinearStateQueryCriteria(null, listOfLinearIds)
        val (states) = serviceHub.vaultService.queryBy(SantaSessionState::class.java, queryCriteria)
        if (states.size < 1) {
            throw IllegalArgumentException("No corresponding GameID Found.")
        }
        santaSessionState = (states[0] as StateAndRef<*>).state.data as SantaSessionState
        return santaSessionState!!
    }

}

@InitiatedBy(CheckAssignedSantaFlow::class)
class CheckAssignedSantaFlowResponder(private val otherSide: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val signedTransaction = subFlow(object : SignTransactionFlow(otherSide) {
            @Suspendable
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
                // Implement responder flow transaction checks here
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSide, signedTransaction.id))
    }
}
