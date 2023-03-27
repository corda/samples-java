package net.corda.samples.tokenizedhouse.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState
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
        val tokenPass = FungibleHouseTokenState(10000, Operator.party,
                UniqueIdentifier(),
                0, "NYCHelena")
        val tokenFail = FungibleHouseTokenState(0, Operator.party,
                UniqueIdentifier(),
                0, "NYCHelena")
        ledgerServices.ledger {
            transaction {
                output(HouseTokenStateContract.CONTRACT_ID, tokenFail)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(HouseTokenStateContract.CONTRACT_ID, tokenPass)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.verifies()
            }
        }
    }
}