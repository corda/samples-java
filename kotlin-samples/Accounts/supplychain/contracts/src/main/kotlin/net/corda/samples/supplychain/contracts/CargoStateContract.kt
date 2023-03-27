package net.corda.samples.supplychain.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class CargoStateContract : Contract{

    companion object{
        const val ID = "net.corda.samples.supplychain.contracts.CargoStateContract"
    }


    override fun verify(tx: LedgerTransaction) {
        requireThat {
            /*
             *
             * For the simplicity of the sample, we unconditionally accept all of the transactions.
             *
             */
        }
    }

    interface Commands : CommandData {
        class Create : CargoStateContract.Commands
    }
}