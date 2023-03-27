package net.corda.samples.duediligence.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.samples.duediligence.contracts.CorporateRecordsContract.Companion.ID
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB"))
    private val ledgerServices = MockServices(TestIdentity(CordaX500Name("TestId", "", "GB")))


    @Test
    fun `Propose Transaction should have zero input`() {
        val  state1 = CorporateRecordsAuditRequest(applicant = alice.party,validater = bob.party,numberOfFiles = 10)
        val  state2 = CorporateRecordsAuditRequest(applicant = alice.party,validater = bob.party,numberOfFiles = 10)
        ledgerServices.ledger {

            // Should fail bid price is equal to previous highest bid
            transaction {
                input(ID,state1)
                output(ID, state2)
                command(listOf(alice.publicKey, bob.publicKey), Commands.Propose())
                fails()
            }
            //pass
            transaction {
                output(ID, state2)
                command(listOf(alice.publicKey, bob.publicKey), Commands.Propose())
                verifies()
            }
        }
    }
}