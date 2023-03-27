package net.corda.samples.carinsurance.states

import org.junit.Assert
import org.junit.Test

class ClaimTests {
    private val desc = "claim description: my car was hit by a blockchain"
    private val claimNumber = "B-132022"
    private val claimAmount = 3000

    @Test
    fun constructorTest() {
        val (claimNumber1, claimDescription, claimAmount1) = Claim(claimNumber, desc, claimAmount)
        Assert.assertEquals(claimNumber, claimNumber1)
        Assert.assertEquals(desc, claimDescription)
        Assert.assertEquals(claimAmount.toLong(), claimAmount1.toLong())
    }
}
