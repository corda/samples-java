package net.corda.samples.auction.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.samples.auction.states.Asset
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.security.PublicKey
import java.util.*

class ContractTests {
    private val alice = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val bob = TestIdentity(CordaX500Name("Bob", "", "GB"))
    private val ledgerServices = MockServices(TestIdentity(CordaX500Name("TestId", "", "GB")))


    @Test
    fun AssetMustHaveDescription() {
        val  statePass = Asset(UniqueIdentifier(),"TestPaint","MadeByPeter","xyz.r3",alice.party)
        val  stateFail = Asset(UniqueIdentifier(),"TestPaint","","xyz.r3",alice.party)


        ledgerServices.ledger {

            // Should fail bid price is equal to previous highest bid
            transaction {
                output(AssetContract.ID, stateFail)
                command(Arrays.asList(alice.publicKey, bob.publicKey), AssetContract.Commands.CreateAsset())
                fails()
            }
            //pass
            transaction {
                output(AssetContract.ID, statePass)
                command(Arrays.asList(alice.publicKey, bob.publicKey), AssetContract.Commands.CreateAsset())
                verifies()
            }
        }
    }
}