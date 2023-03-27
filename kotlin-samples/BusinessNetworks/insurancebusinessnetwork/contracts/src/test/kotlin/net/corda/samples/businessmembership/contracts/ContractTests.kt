package net.corda.samples.businessmembership.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.samples.businessmembership.states.InsuranceState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Test
import java.util.Arrays
import net.corda.testing.node.*


class ContractTests {
    private val ledgerServices = MockServices(
            Arrays.asList("net.corda.samples.businessmembership.contracts"))
    var alice = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var bob = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))

    @Test
    fun failsDueToParticipantsAreNotNetworkMembers() {
        val insurancestate = InsuranceState(alice.party, "TEST", bob.party, UniqueIdentifier().toString(), "Initiating Policy")
        ledgerServices.ledger {
            transaction {
                output(InsuranceStateContract.CONTRACT_NAME,insurancestate)
                command(alice.publicKey, InsuranceStateContract.Commands.Issue())
                this.fails()
            }
        }
    }
}