package net.corda.samples.contractsdk.contracts


import com.r3.corda.lib.contracts.contractsdk.StandardContract
import com.r3.corda.lib.contracts.contractsdk.annotations.*
import com.r3.corda.lib.contracts.contractsdk.verifiers.StandardCommand
import com.r3.corda.lib.contracts.contractsdk.verifiers.StandardState

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.contractsdk.states.RecordPlayerState

/*
You'll notice some nice things about this contract code,
the first is that you can have really small command definitions with a lot of flexibility
the second is that you now have logical grouping of your contract verification and your command definition.

This makes writing your contract code cleaner and more intuitive.
*/
class RecordPlayerContract : StandardContract(), Contract {

    companion object {
        // This id must be used to identify our contract when building a transaction.
        const val ID = "net.corda.samples.contractsdk.contracts.RecordPlayerContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {

        // note the annotations here from the Contracts SDK
        @RequireNumberOfStatesOnInput(value = 0)
        @RequireNumberOfStatesOnOutput(value = 1)
        @RequireSignersFromEachInputState(roles = ["manufacturer"])
        class Issue : Commands

        // with these annotations we can ignore worrying about many
        // aspects of the transaction we're not as interested in.
        @RequireNumberOfStatesOnInput(value = 1)
        @RequireNumberOfStatesOnOutput(value = 1)
        class Update : Commands, StandardCommand {
            // We can add additional logic to the update command without adding extra boilerplate
            override fun verifyFurther(tx: LedgerTransaction) {
                val inputs = tx.inputStates
                val outputs = tx.outputStates
                val oldRp = inputs[0] as RecordPlayerState
                val newRp = outputs[0] as RecordPlayerState

                // We can still use Corda DSL function requireThat to replicate conditions-checks
                requireThat {
                    "Magenetic Strength must be above 0".using(newRp.magneticStrength > 0)
                    "Magenetic Strength is too high".using(newRp.magneticStrength < 100000)
                    "Coil turns can't be negative".using(oldRp.coilTurns > 0)
                    "Coil turns can't be negative".using(newRp.coilTurns > 0)
                    "Coil turns too high".using(newRp.coilTurns < 100000)
                    "songsPlayed should never decrease".using(oldRp.songsPlayed <= newRp.songsPlayed)
                    null
                }
            }
        }
    }
}
