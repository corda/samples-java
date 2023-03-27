package net.corda.samples.bikemarket.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.samples.bikemarket.states.FrameTokenState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()
    val Operator = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))

    //sample tests
    @Test
    fun `SerialNumber Cannot Be Empty`() {
        val tokenPass = FrameTokenState(Operator.party, UniqueIdentifier(), 0, "8742")
        val tokenFail = FrameTokenState(Operator.party, UniqueIdentifier(), 0, "")
        ledgerServices.ledger {
            transaction {
                output(FrameContract.CONTRACT_ID, tokenFail)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(FrameContract.CONTRACT_ID, tokenPass)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.verifies()
            }
        }
    }
}