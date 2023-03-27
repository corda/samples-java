package net.corda.samples.whistleblower.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.samples.whistleblower.states.BlowWhistleState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*


class BlowWhistleContractTests {
    private val a = TestIdentity(CordaX500Name("alice", "", "GB"))
    private val b = TestIdentity(CordaX500Name("bob", "", "GB"))
    private val c = TestIdentity(CordaX500Name("bad corp", "", "GB"))

    private val ledgerServices = MockServices(
            Arrays.asList("net.corda.samples.whistleblower.contracts")
    )

    var st: BlowWhistleState = BlowWhistleState(c.party, a.party.anonymise(), b.party.anonymise())

    // A pre-defined dummy command.
    interface Commands : CommandData {
        class DummyCommand : TypeOnlyCommandData(), Commands
    }


    @Test
    fun SantaSessionContractImplementsContract() {
        assert(BlowWhistleContract() is Contract)
    }

    @Test
    fun contractRequiresZeroInputsInTheTransaction() {
        ledgerServices.ledger {
            transaction {
                // Has an input, will fail.
                output(BLOW_WHISTLE_CONTRACT_ID, st)
                output(BLOW_WHISTLE_CONTRACT_ID, st)
                command(listOf(a.publicKey, b.publicKey), BlowWhistleContract.Commands.BlowWhistleCmd())
                fails()
            }

            transaction {
                // Has no input, will verify.
                output(BLOW_WHISTLE_CONTRACT_ID, st)
                command(listOf(a.publicKey, b.publicKey), BlowWhistleContract.Commands.BlowWhistleCmd())
                verifies()
            }
        }
    }

    @Test
    fun contractRequiresSpecificCommand() {
        ledgerServices.ledger {
            transaction {
                // Has wrong command type, will fail.
                output(BLOW_WHISTLE_CONTRACT_ID, st)
                command(Arrays.asList(a.publicKey, b.publicKey), Commands.DummyCommand())
                fails()
            }

            transaction {
                // Has correct command type, will verify.
                output(BLOW_WHISTLE_CONTRACT_ID, st)
                command(Arrays.asList(a.publicKey, b.publicKey), BlowWhistleContract.Commands.BlowWhistleCmd())
                verifies()
            }
        }
    }
}
