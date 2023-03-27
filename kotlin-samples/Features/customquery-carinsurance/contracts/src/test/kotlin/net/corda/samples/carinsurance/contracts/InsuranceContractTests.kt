package net.corda.samples.carinsurance.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.samples.carinsurance.states.Claim
import net.corda.samples.carinsurance.states.InsuranceState
import net.corda.samples.carinsurance.states.VehicleDetail
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class InsuranceContractTests {
    // A pre-defined dummy command.
    interface Commands : CommandData {
        class DummyCommand : TypeOnlyCommandData(), Commands
    }

    private val ledgerServices = MockServices(
            Arrays.asList("net.corda.samples.carinsurance.contracts")
    )
    private val a = TestIdentity(CordaX500Name("Alice", "", "GB"))
    private val b = TestIdentity(CordaX500Name("Bob", "", "GB"))
    var registrationNumber = "registration number: 2ds9Fvk"
    var chassisNum = "chassis# aedl3sc"
    var make = "Toyota"
    var model = "Corolla"
    var variant = "SE"
    var color = "hot rod beige"
    var fuelType = "regular"
    var vd = VehicleDetail(
            registrationNumber,
            chassisNum,
            make,
            model,
            variant,
            color,
            fuelType)
    var desc = "claim description: my car was hit by a blockchain"
    var claimNumber = "B-132022"
    var claimAmount = 3000
    var c = Claim(claimNumber, desc, claimAmount)

    // in this test scenario, alice is our insurer.
    var policyNum = "R3-Policy-A4byCd"
    var insuredValue = 100000L
    var duration = 50
    var premium = 5
    var insurer = a.party
    var insuree = b.party
    var st = InsuranceState(
            policyNum,
            insuredValue,
            duration,
            premium,
            insurer,
            insuree,
            vd,
            Arrays.asList(c))

    @Test
    fun contractImplementsContract() {
        assert(InsuranceContract() is Contract)
    }

    @Test
    fun contractRequiresOneCommandInTheTransaction() {
        ledgerServices.ledger {
            transaction {
                output(InsuranceContract.ID, st)
                // Has two commands, will fail.
                command(Arrays.asList(a.publicKey, b.publicKey), InsuranceContract.Commands.IssueInsurance())
                command(Arrays.asList(a.publicKey, b.publicKey), InsuranceContract.Commands.IssueInsurance())
                fails()
            }

            transaction {
                output(InsuranceContract.ID, st)
                // Has one command, will verify.
                command(Arrays.asList(a.publicKey, b.publicKey), InsuranceContract.Commands.IssueInsurance())
                verifies()
            }
        }
    }

    @Test
    fun contractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        ledgerServices.ledger {
            transaction {
                // Has wrong command type, will fail.
                output(InsuranceContract.ID, st)
                command(Arrays.asList(a.publicKey), Commands.DummyCommand())
                fails()
            }

            transaction {
                // Has correct command type, will verify.
                output(InsuranceContract.ID, st)
                command(Arrays.asList(a.publicKey, b.publicKey), InsuranceContract.Commands.IssueInsurance())
                verifies()
            }
        }
    }
}
