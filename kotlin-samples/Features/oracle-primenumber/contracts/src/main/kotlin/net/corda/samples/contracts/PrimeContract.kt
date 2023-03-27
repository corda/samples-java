package net.corda.samples.contracts

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.samples.states.PrimeState


class PrimeContract : Contract {

    companion object {
        const val PRIME_PROGRAM_ID: ContractClassName = "net.corda.samples.contracts.PrimeContract"
    }

    // Commands signed by oracles must contain the facts the oracle is attesting to.
    class Create(val n: Int, val nthPrime: Int) : CommandData

    // Our contract does not check that the Nth prime is correct. Instead, it checks that the
    // information in the command and state match.
    override fun verify(tx: LedgerTransaction) = requireThat {
        "There are no inputs" using (tx.inputs.isEmpty())
        val output = tx.outputsOfType<PrimeState>().single()
        val command = tx.commands.requireSingleCommand<Create>().value
        "The prime in the output does not match the prime in the command." using
                (command.n == output.n && command.nthPrime == output.nthPrime)
    }
}


