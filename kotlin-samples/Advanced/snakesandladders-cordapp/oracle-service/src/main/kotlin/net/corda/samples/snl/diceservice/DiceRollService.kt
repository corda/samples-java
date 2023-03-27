package net.corda.samples.snl.diceservice


import net.corda.core.crypto.TransactionSignature
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.FilteredTransactionVerificationException
import java.util.HashMap
import java.util.Random


@CordaService
class DiceRollService(serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    private val serviceHub: AppServiceHub
    private val rollMap: MutableMap<String, Int>
    fun diceRoll(player: String): Int {
        val ran = Random()
        val roll = ran.nextInt(6) + 1
        rollMap[player] = roll
        return roll
    }

    @Throws(FilteredTransactionVerificationException::class)
    fun sign(transaction: FilteredTransaction): TransactionSignature {
        transaction.verify()
        val isValid = true // Additional verification logic here.
        return if (isValid) {
            serviceHub.createSignature(transaction, serviceHub.myInfo.legalIdentities.get(0).owningKey)
        } else {
            throw IllegalArgumentException()
        }
    }

    init {
        this.serviceHub = serviceHub
        rollMap = HashMap()
    }
}
