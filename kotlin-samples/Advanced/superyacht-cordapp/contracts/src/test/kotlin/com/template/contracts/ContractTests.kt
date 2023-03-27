package com.template.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import com.template.states.YachtState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Contract
import net.corda.core.contracts.UniqueIdentifier
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.test.assertTrue

class ContractTests {
    private val ledgerServices: MockServices = MockServices()

    private val boatIntl = TestIdentity(CordaX500Name("Boat International", "London", "GB") )
    private val alice = TestIdentity(CordaX500Name("Alice", "London", "GB") )
    private val bob = TestIdentity(CordaX500Name("Bob", "London", "GB") )

    private val mockYachtState = YachtState(
        boatIntl.party,
        alice.party,
        "World Traveller",
        "Motor Yacht",
        12.15,
        "Burgess",
        2008,
        Amount(6000000, BigDecimal("1"), Currency.getInstance("USD")),
        true,
        UniqueIdentifier(),
        listOf(alice.party)
    )

    private val mockYachtStateBob = YachtState(
        boatIntl.party,
        bob.party,
        "World Traveller",
        "Motor Yacht",
        12.15,
        "Burgess",
        2008,
        Amount(6000000, BigDecimal("1"), Currency.getInstance("USD")),
        true,
        UniqueIdentifier(),
        listOf(bob.party)
    )

    private val mockYachtStateNFS = YachtState(
        boatIntl.party,
        bob.party,
        "World Traveller",
        "Motor Yacht",
        12.15,
        "Burgess",
        2008,
        Amount(6000000, BigDecimal("1"), Currency.getInstance("USD")),
        false,
        UniqueIdentifier(),
        listOf(bob.party)
    )

    /* YACHT STATE TESTS */

    @Test
    fun yachtContractImplementsContract(){
        assertTrue(YachtContract() is Contract)
    }

    // Tests for Create Command
    @Test
    fun yachtContractCreateCommandShouldHaveNoInputs() {
        ledgerServices.ledger {
            transaction {
                input(YachtContract.ID, mockYachtState)
                output(YachtContract.ID, mockYachtState)
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                fails()
            }
            transaction {
                output(YachtContract.ID, mockYachtState)
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun yachtContractCreateCommandShouldOnlyHaveOneOutput(){
        ledgerServices.ledger{
            transaction{
                output(YachtContract.ID, mockYachtState)
                output(YachtContract.ID, mockYachtState)
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                fails()
            }
            transaction{
                output(YachtContract.ID, mockYachtState)
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun yachtContractCreateCommandRequiresOneCommand(){
        ledgerServices.ledger{
            transaction{
                output(YachtContract.ID, mockYachtState)
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                fails()
            }
            transaction{
                output(YachtContract.ID, mockYachtState)
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun yachtContractCreateCommandMustHaveTheOwnerAsRequiredSigners(){
        ledgerServices.ledger{
            transaction{
                output(YachtContract.ID, mockYachtState)
                command(listOf(bob.publicKey), YachtContract.Commands.Create())
                fails()
            }
            transaction{
                output(YachtContract.ID, mockYachtState)
                command(listOf(boatIntl.publicKey), YachtContract.Commands.Create())
                fails()
            }
            transaction{
                output(YachtContract.ID, mockYachtState)
                command(listOf(alice.publicKey), YachtContract.Commands.Create())
                verifies()
            }
        }
    }
}
