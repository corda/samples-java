package com.tutorial.contracts

import com.tutorial.states.AppleStamp
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import com.tutorial.states.TemplateState
import net.corda.core.contracts.UniqueIdentifier
import java.security.PublicKey
import java.util.*

class ContractTests {
    private val ledgerServices: MockServices = MockServices(listOf("com.tutorial"))
    var alice = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))
    var bob = TestIdentity(CordaX500Name("Alice", "TestLand", "US"))

    @Test
    fun issuerAndRecipientCannotHaveSameEmail() {
        val state = TemplateState("Hello-World", alice.party, bob.party)
        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                //failing transaction
                input(TemplateContract.ID, state)
                output(TemplateContract.ID, state)
                command(alice.publicKey, TemplateContract.Commands.Create())
                fails()
            }
            //pass
            transaction {
                //passing transaction
                output(TemplateContract.ID, state)
                command(alice.publicKey, TemplateContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun stampIssuanceCanOnlyHaveOneOutput(){
        val stamp = AppleStamp("FUji4072", alice.party, bob.party, UniqueIdentifier())
        val stamp2 = AppleStamp("HoneyCrispy7864", alice.party, bob.party, UniqueIdentifier())

        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                //failing transaction
                output(AppleStampContract.ID, stamp)
                output(AppleStampContract.ID, stamp2)
                command(alice.publicKey, AppleStampContract.Commands.Issue())
                fails()
            }
            //pass
            transaction {
                //passing transaction
                output(AppleStampContract.ID, stamp)
                command(alice.publicKey, AppleStampContract.Commands.Issue())
                verifies()
            }
        }
    }

    @Test
    fun StampMustHaveDescription(){
        val stamp = AppleStamp("", alice.party, bob.party, UniqueIdentifier())
        val stamp2 = AppleStamp("FUji4072", alice.party, bob.party, UniqueIdentifier())

        ledgerServices.ledger {
            // Should fail bid price is equal to previous highest bid
            transaction {
                //failing transaction
                output(AppleStampContract.ID, stamp)
                command(Arrays.asList(alice.publicKey, bob.publicKey), AppleStampContract.Commands.Issue())
                fails()
            }
            //pass
            transaction {
                //passing transaction
                output(AppleStampContract.ID, stamp2)
                command(Arrays.asList(alice.publicKey, bob.publicKey), AppleStampContract.Commands.Issue())
                verifies()
            }
        }
    }





}