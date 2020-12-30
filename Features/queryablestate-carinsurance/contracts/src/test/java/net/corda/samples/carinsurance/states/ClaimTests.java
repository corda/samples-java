package net.corda.samples.carinsurance.states;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClaimTests {

    private final String desc = "claim description: my car was hit by a blockchain";
    private final String claimNumber = "B-132022";
    private final int claimAmount = 3000;

    @Test
    public void constructorTest() {
        Claim st = new Claim(claimNumber, desc, claimAmount);

        assertEquals(claimNumber, st.getClaimNumber());
        assertEquals(desc, st.getClaimDescription());
        assertEquals(claimAmount, st.getClaimAmount());
    }

}
