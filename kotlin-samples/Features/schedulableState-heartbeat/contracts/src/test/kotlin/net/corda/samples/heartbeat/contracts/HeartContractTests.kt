package net.corda.samples.heartbeat.contracts

import net.corda.core.contracts.Contract
import net.corda.core.identity.CordaX500Name
import net.corda.samples.heartbeat.states.HeartState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class HeartContractTests {
    private val a = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val b = TestIdentity(CordaX500Name("Bob", "", "GB"))

    private val ledgerServices = MockServices(
            Arrays.asList("net.corda.samples.heartbeat.contracts")
    )
    var st: HeartState = HeartState(a.party)

    @Test
    fun contractImplementsContract() {
        assert(HeartContract() is Contract)
    }

    @Test
    fun contractRequiresSpecificCommand() {
        ledgerServices.ledger {
            transaction {
                // Has correct command type, will verify.
                output(HeartContract.contractID, st)
                command(Arrays.asList(a.publicKey), HeartContract.Commands.Beat())
                verifies()
            }
        }
    }
}
