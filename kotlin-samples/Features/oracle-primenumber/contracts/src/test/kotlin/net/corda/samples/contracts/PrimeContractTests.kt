package net.corda.samples.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.samples.states.PrimeState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Assert
import org.junit.Test
import java.util.*

class PrimeContractTests {

    // A pre-defined dummy command.
    interface Commands : CommandData {
        class DummyCommand : TypeOnlyCommandData(), Commands
    }

    private val ledgerServices = MockServices(
            Arrays.asList("net.corda.samples.contracts")
    )

    private val a = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val b = TestIdentity(CordaX500Name("Bob", "", "GB"))
    private val st = PrimeState(1, 5, a.party)

    @Test
    fun contractImplementsContract() {
        Assert.assertTrue(PrimeContract() is Contract)
    }

    @Test
    fun constructorTest() {
        Assert.assertEquals(1, st.n)
        Assert.assertEquals(5, st.nthPrime)
    }

    @Test
    fun contractRequiresOneCommandInTheTransaction() {
        ledgerServices.ledger {
            transaction {
                output(PrimeContract.PRIME_PROGRAM_ID, st)
                // Has two commands, will fail.
                command(Arrays.asList(a.publicKey), PrimeContract.Create(1, 5))
                command(Arrays.asList(a.publicKey), PrimeContract.Create(1, 5))
                fails()
            }
            transaction {
                output(PrimeContract.PRIME_PROGRAM_ID, st)
                // Has one command, will verify.
                command(Arrays.asList(a.publicKey), PrimeContract.Create(1, 5))
                verifies()
            }
        }
    }

    @Test
    fun contractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        ledgerServices.ledger {

            transaction {
                // Has wrong command type, will fail.
                output(PrimeContract.PRIME_PROGRAM_ID, st)
                command(Arrays.asList(a.publicKey), Commands.DummyCommand())
                fails()
            }

            transaction {
                // Has correct command type, will verify.
                output(PrimeContract.PRIME_PROGRAM_ID, st)
                command(Arrays.asList(a.publicKey), PrimeContract.Create(1, 5))
                verifies()
            }
        }
    }
}
