package com.tutorial.contracts

import com.tutorial.states.AppleStamp
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

//Domain Specific Language
class AppleStampContract : Contract {
    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {

        //Extract the command from the transaction.
        val commandData = tx.commands[0].value

        //Verify the transaction according to the intention of the transaction
        when (commandData) {
            is Commands.Issue -> requireThat {
                val output = tx.outputsOfType(AppleStamp::class.java)[0]
                "This transaction should only have one AppleStamp state as output".using(tx.outputs.size == 1)
                "The output AppleStamp state should have clear description of the type of redeemable goods".using(output.stampDesc != "")
                null
            }
            is BasketOfApplesContract.Commands.Redeem-> requireThat {
                //Transaction verification will happen in BasketOfApples Contract
            }
    }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        //In our hello-world app, We will have two commands.
        class Issue : Commands
    }

    companion object {
        // This is used to identify our contract when building a transaction.
        const val ID = "com.tutorial.contracts.AppleStampContract"
    }
}