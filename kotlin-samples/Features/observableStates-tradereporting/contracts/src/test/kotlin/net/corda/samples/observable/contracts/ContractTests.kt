package net.corda.samples.observable.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.samples.observable.states.HighlyRegulatedState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()
    val partya = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))
    var partyb = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))

    @Test
    fun `No Negative PayCheck Value`() {
        val tokenPass = HighlyRegulatedState(partya.party, partyb.party)
        val tokenFail = HighlyRegulatedState(partyb.party, partyb.party)

        ledgerServices.ledger {
            transaction {
                output(HighlyRegulatedContract.ID, tokenFail)
                command(partya.publicKey, HighlyRegulatedContract.Commands.Trade())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(HighlyRegulatedContract.ID, tokenPass)
                command(partya.publicKey, HighlyRegulatedContract.Commands.Trade())
                this.verifies()
            }
        }
    }
}