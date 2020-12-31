package net.corda.samples.carinsurance.states;

import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import java.util.Arrays;

import static org.jgroups.util.Util.assertEquals;
import static org.jgroups.util.Util.assertTrue;

public class InsuranceStateTests {

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

        String desc = "claim description: my car was hit by a blockchain";
        String claimNumber = "B-132022";
        int claimAmount = 3000;

        Claim c = new Claim(claimNumber, desc, claimAmount);

        // in this test scenario, alice is our insurer.
        String policyNum = "R3-Policy-A4byCd";
        long insuredValue = 100000L;
        int duration = 50;
        int premium = 5;
        Party insurer = a.getParty();
        Party insuree = b.getParty();

        InsuranceState st = new InsuranceState(
                policyNum,
                insuredValue,
                duration,
                premium,
                insurer,
                insuree,
                vd,
                Arrays.asList(c));

        assertEquals(policyNum, st.getPolicyNumber());
        assertEquals(insuredValue, st.getInsuredValue());
        assertEquals(duration, st.getDuration());
        assertEquals(premium, st.getPremium());
        assertEquals(insurer, st.getInsurer());
        assertEquals(insuree, st.getInsuree());
        assertEquals(vd, st.getVehicleDetail());

        assertTrue(st.getParticipants().contains(a.getParty()));
        assertTrue(st.getParticipants().contains(b.getParty()));
    }

}
