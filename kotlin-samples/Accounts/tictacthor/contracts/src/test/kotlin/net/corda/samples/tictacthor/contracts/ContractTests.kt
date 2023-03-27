package net.corda.samples.tictacthor.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.samples.tictacthor.states.BoardState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val ledgerServices = MockServices()
    val Operator = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))
    var Operator2 = TestIdentity(CordaX500Name("Bob", "TestLand", "US"))

    @Test
    fun `Game Can Only Created When Two Different Player Presented`() {
        val playerX = UniqueIdentifier()
        val tokenPass = BoardState(UniqueIdentifier(), UniqueIdentifier(),
                AnonymousParty(Operator.publicKey), AnonymousParty(Operator2.publicKey))
        val tokenFail = BoardState(playerX, playerX,
                AnonymousParty(Operator.publicKey), AnonymousParty(Operator2.publicKey))
        ledgerServices.ledger {
            transaction {
                output(BoardContract.ID, tokenFail)
                command(Operator.publicKey, BoardContract.Commands.StartGame())
                this.fails()
            }
        }
        ledgerServices.ledger {
            transaction {
                output(BoardContract.ID, tokenPass)
                command(Operator.publicKey, BoardContract.Commands.StartGame())
                this.verifies()
            }
        }
    }
}