package net.corda.samples.snl.oracle.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.unwrap
import net.corda.samples.snl.diceservice.DiceRollService


@InitiatingFlow
class OracleSignatureFlow(private val oracle: Party, private val ftx: FilteredTransaction) : FlowLogic<TransactionSignature?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call() = initiateFlow(oracle).sendAndReceive(TransactionSignature::class.java, ftx).unwrap { it }

}

@InitiatedBy(OracleSignatureFlow::class)
class OracleSignatureFlowHandler(private val requestSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Unit {
        val transaction = requestSession.receive(FilteredTransaction::class.java).unwrap{ it-> it }
        var signature: TransactionSignature? = null
        signature = try {
            serviceHub.cordaService(DiceRollService::class.java).sign(transaction)
        } catch (e: Exception) {
            throw FlowException(e)
        }
        requestSession.send(signature)
        return
    }
}
