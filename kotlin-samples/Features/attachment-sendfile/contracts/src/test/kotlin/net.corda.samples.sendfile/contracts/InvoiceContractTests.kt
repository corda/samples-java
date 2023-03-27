package net.corda.samples.sendfile.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.samples.sendfile.states.InvoiceState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test


class InvoiceContractTests {
    // A pre-defined dummy command.
    interface Commands : CommandData {
        class DummyCommand : TypeOnlyCommandData(), Commands
    }

    private val ledgerServices = MockServices(
            listOf("net.corda.samples.sendfile.contracts")
    )

    private val a = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val b = TestIdentity(CordaX500Name("Bob", "", "GB"))
    private val STRINGID = "StringID that is unique"
    private val st = InvoiceState(STRINGID, listOf(a.party, b.party))

    @Test
    fun InvoiceContractImplementsContract() {
        assert(InvoiceContract() is Contract)
    }

    @Test
    fun contractRequiresOneCommandInTheTransaction() {
        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, st)
                // Has two commands, will fail.
                command(listOf(a.publicKey, b.publicKey), InvoiceContract.Commands.Issue())
                command(listOf(a.publicKey, b.publicKey), InvoiceContract.Commands.Issue())
                this.fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, st)
                command(listOf(a.publicKey, b.publicKey), InvoiceContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun contractRequiresTheTransactionsCommandToBeAnIssueCommand() {

        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, st)
                // Has dummy command, will fail.
                command(listOf(a.publicKey, b.publicKey), Commands.DummyCommand())

                this.fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                output(InvoiceContract.ID, st)
                // Has two commands, will fail.
                command(listOf(a.publicKey, b.publicKey), InvoiceContract.Commands.Issue())

                this.verifies()
            }
        }
    }
}

