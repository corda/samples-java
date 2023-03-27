package net.corda.samples.avatar.contracts

import net.corda.samples.avatar.states.Avatar
import net.corda.samples.avatar.states.Expiry
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class AvatarContract: Contract {

    companion object {
        // Used to identify our contract when building a transaction.
        const val AVATAR_CONTRACT_ID = "net.corda.samples.avatar.contracts.AvatarContract"
    }

    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val commandWithParties = tx.commands.requireSingleCommand(Commands::class.java)
        val signers = commandWithParties.signers

        when (commandWithParties.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed when sending the Hello-World message.".using(tx.inputs.isEmpty())
                "There should be 0 input states.".using(tx.inputs.isEmpty())
                "There should be 2 output states.".using(tx.outputStates.size == 2)
                "There should be 1 expiry state.".using(tx.outputsOfType(Expiry::class.java).size == 1)
                "There shoule be 1 Avatar created.".using(tx.outputsOfType(Avatar::class.java).size == 1)

                val avatar = tx.outputsOfType(Avatar::class.java)[0]
                "Avatar Owner must always sign the newly created Avatar.".using(signers.contains(avatar.owner.owningKey))

                val avatarEncumbrance = tx.outputs.first { it.data is Avatar }.encumbrance
                "Avatar needs to be encumbered".using(avatarEncumbrance != null)
            }

            is Commands.Transfer -> requireThat{
                "There should be 2 inputs.".using(tx.inputs.size == 2)
                "There must be 1 expiry as an input.".using(tx.inputsOfType(Expiry::class.java).size == 1)
                "There must be 1 avatar as an input".using(tx.inputsOfType(Avatar::class.java).size == 1)

                "There should be two output states".using(tx.inputs.size == 2)
                "There should be 1 expiry state.".using(tx.outputsOfType(Expiry::class.java).size == 1)
                "There shoule be 1 Avatar created.".using(tx.outputsOfType(Avatar::class.java).size == 1)

                val newAvatar = tx.outputsOfType(Avatar::class.java).stream().findFirst().orElseThrow {
                    IllegalArgumentException(
                        "No Avatar created for transferring."
                    )
                }

                val oldAvatar = tx.inputsOfType(Avatar::class.java).stream().findFirst().orElseThrow {
                    IllegalArgumentException(
                        "Existing Avatar to transfer not found."
                    )
                }

                "New and old Avatar must just have the owners changed.".using(newAvatar.equals(oldAvatar))
                "New Owner should sign the new Avatar".using(signers.contains(newAvatar.owner.owningKey))
                "Old owner must sign the old Avatar".using(signers.contains(oldAvatar.owner.owningKey))
            }
        }
    }

    interface Commands : CommandData {
        class Create : Commands
        class Transfer : Commands
    }
}