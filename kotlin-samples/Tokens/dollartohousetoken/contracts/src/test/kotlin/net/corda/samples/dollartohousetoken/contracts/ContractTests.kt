package net.corda.samples.dollartohousetoken.contracts

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.samples.dollartohousetoken.states.HouseState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class ContractTests {
    private val ledgerServices = MockServices()
    val Operator = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))

    //sample tests
    @Test
    fun `SerialNumber Cannot Be Empty`() {
        val tokenPass = HouseState(UniqueIdentifier(),
                Arrays.asList(Operator.party),
                Amount.parseCurrency("1000 USD"),
                10, "500sqft",
                "none", "NYC")
        val tokenFail = HouseState(UniqueIdentifier(),
                Arrays.asList(Operator.party),
                Amount.parseCurrency("0 USD"),
                10, "500sqft",
                "none", "NYC")
        ledgerServices.ledger {
            transaction {
                output(HouseContract.CONTRACT_ID, tokenFail)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(HouseContract.CONTRACT_ID, tokenPass)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.verifies()
            }
        }
    }
}