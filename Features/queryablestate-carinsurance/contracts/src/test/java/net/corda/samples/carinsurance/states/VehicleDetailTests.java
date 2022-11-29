package net.corda.samples.carinsurance.states;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VehicleDetailTests {

    TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));


    @Test
    public void constructorTest() {

        String registrationNumber = "registration number: 2ds9Fvk";
        String chassisNum = "chassis# aedl3sc";
        String make = "Toyota";
        String model = "Corolla";
        String variant = "SE";
        String color = "hot rod beige";
        String fuelType = "regular";

        VehicleDetail vd = new VehicleDetail(
                registrationNumber,
                chassisNum,
                make,
                model,
                variant,
                color,
                fuelType);

        assertEquals(registrationNumber, vd.getRegistrationNumber());
        assertEquals(chassisNum, vd.getChasisNumber());
        assertEquals(make, vd.getMake());
        assertEquals(model, vd.getModel());
        assertEquals(variant, vd.getVariant());
        assertEquals(color, vd.getColor());
        assertEquals(fuelType, vd.getFuelType());
    }

}
