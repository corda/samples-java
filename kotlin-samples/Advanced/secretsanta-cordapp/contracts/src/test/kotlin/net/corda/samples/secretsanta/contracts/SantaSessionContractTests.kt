package net.corda.samples.secretsanta.contracts

import junit.framework.TestCase
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.samples.secretsanta.states.SantaSessionState
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.TransactionDSL
import net.corda.testing.dsl.TransactionDSLInterpreter
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import java.util.*


class SantaSessionContractTests {
    private val santa = TestIdentity(CordaX500Name("Santa", "", "GB"))
    private val elf = TestIdentity(CordaX500Name("Santa", "", "GB"))

    // A pre-defined dummy command.
    interface Commands : CommandData {
        class DummyCommand : TypeOnlyCommandData(), Commands
    }

    private val ledgerServices = MockServices(Arrays.asList("net.corda.samples.secretsanta.contracts"))

    private val playerNames = ArrayList(Arrays.asList("david", "alice", "bob", "charlie", "olivia", "peter"))
    private val playerEmails = ArrayList(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "charlie@corda.net", "olivia@corda.net", "peter@corda.net"))
    private val st = SantaSessionState(playerNames, playerEmails, santa.party, elf.party)
    private val noPlayerNames = ArrayList<String>()
    private val onePlayerNames = ArrayList(Arrays.asList("david"))
    private val threePlayerNames = ArrayList(Arrays.asList("david", "alice", "bob", "peter"))
    private val noPlayerEmails = ArrayList<String>()
    private val onePlayerEmails = ArrayList(Arrays.asList("david@corda.net"))
    private val threePlayerEmails = ArrayList(Arrays.asList("david@corda.net", "alice@corda.net", "bob@corda.net", "peter@corda.net"))
    @Before
    fun setup() {
    }

    //    @After
//    public void tearDown() { }

    @Test
    fun SantaSessionContractImplementsContract() {
        assert(SantaSessionContract() is Contract)
    }

    @Test
    fun constructorTest() {
        val st = SantaSessionState(playerNames, playerEmails, santa.party, elf.party)
        TestCase.assertEquals(santa.party, st.issuer)
        TestCase.assertEquals(playerNames, st.playerNames)
        TestCase.assertEquals(playerEmails, st.playerEmails)
    }

    // TODO
    @Test
    fun SantaSessionContractRequiresZeroInputsInTheTransaction() {
        val t1 = SantaSessionState(playerNames, playerEmails, santa.party, elf.party)
        val t2 = SantaSessionState(playerNames, playerEmails, santa.party, elf.party)

        ledgerServices.ledger {
            transaction {
                // Has an input, will fail.
                input(SantaSessionContract.ID, t1)
                output(SantaSessionContract.ID, t2)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.fails()

            }
            transaction {
                // Has no input, will verify.
                output(SantaSessionContract.ID, st)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun SantaSessionContractRequiresOneOutputInTheTransaction() {
        ledgerServices.ledger {
            transaction {
                // Has two outputs, will fail.
                output(SantaSessionContract.ID, st)
                output(SantaSessionContract.ID, st)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.fails()

            }
            transaction {
                // Has one output, will verify.
                output(SantaSessionContract.ID, st)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.verifies()
            }
        }
    }

    @Test
    fun SantaSessionContractRequiresOneCommandInTheTransaction() {
        ledgerServices.ledger {
            transaction {
                output(SantaSessionContract.ID, st)
                // Has two commands, will fail.
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.fails()
            }
            transaction {
                output(SantaSessionContract.ID, st)
                // Has one command, will verify.
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.verifies()

            }
        }
    }

    @Test
    fun SantaSessionContractRequiresTheTransactionsOutputToBeASantaSessionState() {
        ledgerServices.ledger {
            transaction{
                // Has wrong output type, will fail.
                output(SantaSessionContract.ID, DummyState())
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.fails()
            }
            transaction {
                // Has correct output type, will verify.
                output(SantaSessionContract.ID, st)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.verifies()

            }
        }
    }

    @Test
    fun SantaSessionContractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        ledgerServices.ledger {
            transaction {
                // Has wrong command type, will fail.
                output(SantaSessionContract.ID, st)
                command(Arrays.asList(santa.publicKey), Commands.DummyCommand())
                this.fails()

            }
            transaction {
                // Has correct command type, will verify.
                output(SantaSessionContract.ID, st)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.verifies()

            }
        }
    }

    @Test
    fun SantaSessionContractRequiresTheTransactionsOutputToHaveMoreThanThreePlayers() {
        val st = SantaSessionState(playerNames, playerEmails, santa.party, elf.party)
        val threePlayerSantaState = SantaSessionState(threePlayerNames, threePlayerEmails, santa.party, elf.party)

        ledgerServices.ledger {
            transaction {
                // Has three players SantaSessionState, will verify.
                output(SantaSessionContract.ID, threePlayerSantaState)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.verifies()
            }
            transaction {
                // Has over three players SantaSessionState, will verify.
                output(SantaSessionContract.ID, st)
                command(Arrays.asList(santa.publicKey, elf.publicKey), SantaSessionContract.Commands.Issue())
                this.verifies()

            }
        }
    }
}
