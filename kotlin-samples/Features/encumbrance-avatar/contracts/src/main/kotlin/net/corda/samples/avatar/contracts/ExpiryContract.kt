package net.corda.samples.avatar.contracts

import net.corda.samples.avatar.states.Expiry
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.time.LocalDateTime
import java.time.ZoneId

//ExpiryContract is also run when Avatar's contract is run
class ExpiryContract : Contract {

    companion object {
        const val EXPIRY_CONTRACT_ID = "net.corda.samples.avatar.contracts.ExpiryContract"
    }

    @Throws(IllegalArgumentException::class)
    override fun verify(tx: LedgerTransaction) {
        val commandWithParties = tx.commands.requireSingleCommand(AvatarContract.Commands::class.java)

        var expiry: Expiry = tx.outputsOfType(Expiry::class.java)[0]
        when (commandWithParties.value) {
            is AvatarContract.Commands.Transfer -> requireThat{
                expiry = tx.inputsOfType(Expiry::class.java)[0]
            }
        }

        val timeWindow = tx.timeWindow
        if (timeWindow?.untilTime == null) {
            throw IllegalArgumentException("Make sure you specify the time window for the Avatar transaction.")
        }

        //Expiry time should be after the time window, if the avatar expires before the time window, then the avatar
        //cannot be sold
        if (timeWindow.untilTime!!.isAfter(expiry.expiry)) {
            throw IllegalArgumentException("Avatar transfer time has expired! Expiry date & time was: " + LocalDateTime.ofInstant(expiry.expiry, ZoneId.systemDefault()))
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Pass : Commands
    }
}