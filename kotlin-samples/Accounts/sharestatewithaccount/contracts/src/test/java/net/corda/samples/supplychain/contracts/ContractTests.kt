package net.corda.samples.supplychain.contracts


import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.samples.supplychain.states.InvoiceState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()
    val Operator = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))
    var Operator2 = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))

    @Test
    fun `Invoice Amount Must Be Greater Than Zero`() {
        val tokenPass = InvoiceState(10, AnonymousParty(Operator.publicKey), AnonymousParty(Operator2.publicKey), UniqueIdentifier().id)
        val tokenFail = InvoiceState(-1, AnonymousParty(Operator.publicKey), AnonymousParty(Operator2.publicKey), UniqueIdentifier().id)
        ledgerServices.ledger {
            transaction {
                output(InvoiceStateContract.ID, tokenFail)
                command(Operator.publicKey, InvoiceStateContract.Commands.Create())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(InvoiceStateContract.ID, tokenPass)
                command(Operator.publicKey, InvoiceStateContract.Commands.Create())
                this.verifies()
            }
        }
    }
}