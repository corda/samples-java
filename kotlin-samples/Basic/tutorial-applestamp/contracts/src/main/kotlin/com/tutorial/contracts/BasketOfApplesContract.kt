package com.tutorial.contracts

import com.tutorial.states.AppleStamp
import com.tutorial.states.BasketOfApples
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class BasketOfApplesContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        //Extract the command from the transaction.
        val commandData = tx.commands[0].value
        val output = tx.outputsOfType(BasketOfApples::class.java)[0]

        when (commandData) {
            is Commands.packBasket -> requireThat {
                "This transaction should only output one BasketOfApples state".using(tx.outputs.size == 1)
                "The output BasketOfApples state should have clear description of Apple product".using(output.description != "")
                "The output BasketOfApples state should have non zero weight".using(output.weight > 0)
                null
            }
            is Commands.Redeem -> requireThat {
                val input = tx.inputsOfType(AppleStamp::class.java)[0]
                "This transaction should consume two states".using(tx.inputStates.size == 2)
                "The issuer of the Apple stamp should be the producing farm of this basket of apple".using(
                    input.issuer.equals(output.farm)
                )
                "The basket of apple has to weight more than 0".using(output.weight > 0)
                null
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class packBasket : Commands
        class Redeem : Commands
    }

    companion object {
        // This is used to identify our contract when building a transaction.
        const val ID = "com.tutorial.contracts.BasketOfApplesContract"
    }
}