package net.corda.samples.stockpaydividend.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.samples.stockpaydividend.states.StockState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.math.BigDecimal
import java.util.*

class ContractTests {
    private val ledgerServices = MockServices()
    val Operator = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))

    //sample tests
    @Test
    fun `multiple Output Tests`() {
        val tokenPass = StockState(Operator.party, "TT", "Test Token",
                "USD", BigDecimal.valueOf(2.7), BigDecimal.valueOf(0), Date(), Date(), UniqueIdentifier()
        )
        val tokenFail = StockState(Operator.party, "", "Test Token",
                "USD", BigDecimal.valueOf(2.7), BigDecimal.valueOf(0.7), Date(), Date(), UniqueIdentifier()
        )
        ledgerServices.ledger {
            transaction {
                output(StockContract.CONTRACT_ID, tokenFail)
                output(StockContract.CONTRACT_ID, tokenFail)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(StockContract.CONTRACT_ID, tokenPass)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.verifies()
            }
        }
    }
}