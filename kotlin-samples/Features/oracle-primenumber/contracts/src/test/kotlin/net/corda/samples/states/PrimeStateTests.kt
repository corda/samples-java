package net.corda.samples.states

import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Assert
import org.junit.Test

class PrimeStateTests {

    var a = TestIdentity(CordaX500Name("Alice", "", "GB"))

    @Test
    fun constructorTest() {
        val st = PrimeState(1, 5, a.party)
        Assert.assertEquals(a.party, st.requester)
        Assert.assertTrue(st.participants.contains(a.party))
    }
}
