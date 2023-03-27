package net.corda.samples.tokentofriend.contracts

import com.r3.corda.lib.tokens.contracts.commands.EvolvableTokenTypeCommand
import com.r3.corda.lib.tokens.contracts.commands.Update
import net.corda.samples.tokentofriend.states.CustomTokenState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()
    val Operator = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))

    @Test
    fun `issuer and recipient cannot have same email`() {
        val tokenPass = CustomTokenState("Peter@gmail.com","David@gmail.com","Corda Number 1!", Operator.party,0,UniqueIdentifier())
        val tokenFail = CustomTokenState("Peter@gmail.com","Peter@gmail.com","Corda Number 1!", Operator.party,0,UniqueIdentifier())
        ledgerServices.ledger {
            transaction {
                output(CustomTokenContract.CONTRACT_ID,tokenFail)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(CustomTokenContract.CONTRACT_ID,tokenPass)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.verifies()
            }
        }
    }
    @Test
    fun `recipient email cannot be empty`() {
        val tokenPass = CustomTokenState("Peter@gmail.com","David@gmail.com","Corda Number 1!", Operator.party,0,UniqueIdentifier())
        val tokenFail = CustomTokenState("Peter@gmail.com","","Corda Number 1!", Operator.party,0,UniqueIdentifier())
        ledgerServices.ledger {
            transaction {
                output(CustomTokenContract.CONTRACT_ID,tokenFail)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(CustomTokenContract.CONTRACT_ID,tokenPass)
                command(Operator.publicKey, com.r3.corda.lib.tokens.contracts.commands.Create())
                this.verifies()
            }
        }
    }
}