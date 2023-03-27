package net.corda.samples.carinsurance.states

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import org.junit.Assert
import java.util.*

class InsuranceStateTests {

    var a = TestIdentity(CordaX500Name("Alice", "", "GB"))
    var b = TestIdentity(CordaX500Name("Bob", "", "GB"))

    @Test
    fun constructorTest() {
        val registrationNumber = "registration number: 2ds9Fvk"
        val chassisNum = "chassis# aedl3sc"
        val make = "Toyota"
        val model = "Corolla"
        val variant = "SE"
        val color = "hot rod beige"
        val fuelType = "regular"
        val vd = VehicleDetail(
                registrationNumber,
                chassisNum,
                make,
                model,
                variant,
                color,
                fuelType)
        val desc = "claim description: my car was hit by a blockchain"
        val claimNumber = "B-132022"
        val claimAmount = 3000
        val c = Claim(claimNumber, desc, claimAmount)

        // in this test scenario, alice is our insurer.
        val policyNum = "R3-Policy-A4byCd"
        val insuredValue = 100000L
        val duration = 50
        val premium = 5
        val insurer = a.party
        val insuree = b.party
        val (policyNumber, insuredValue1, duration1, premium1, insurer1, insuree1, vehicleDetail, _, participants) = InsuranceState(
                policyNum,
                insuredValue,
                duration,
                premium,
                insurer,
                insuree,
                vd,
                Arrays.asList(c))

        Assert.assertEquals(policyNum, policyNumber)
        Assert.assertEquals(insuredValue, insuredValue1)
        Assert.assertEquals(duration, duration1)
        Assert.assertEquals(premium, premium1)
        Assert.assertEquals(insurer, insurer1)
        Assert.assertEquals(insuree, insuree1)
        Assert.assertEquals(vd, vehicleDetail)
        Assert.assertTrue(participants.contains(a.party))
        Assert.assertTrue(participants.contains(b.party))
    }
}
