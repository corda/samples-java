package net.corda.samples.carinsurance.states

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Test
import org.junit.Assert

class VehicleDetailTests {

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

        val (registrationNumber1, chasisNumber, make1, model1, variant1, color1, fuelType1) = VehicleDetail(
                registrationNumber,
                chassisNum,
                make,
                model,
                variant,
                color,
                fuelType)

        Assert.assertEquals(registrationNumber, registrationNumber1)
        Assert.assertEquals(chassisNum, chasisNumber)
        Assert.assertEquals(make, make1)
        Assert.assertEquals(model, model1)
        Assert.assertEquals(variant, variant1)
        Assert.assertEquals(color, color1)
        Assert.assertEquals(fuelType, fuelType1)
    }
}
