package net.corda.samples.referencestates.states

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import org.junit.Assert
import org.junit.Test

class SanctionableIOUStateTests {
    var a = TestIdentity(CordaX500Name("Alice", "", "GB"))
    var b = TestIdentity(CordaX500Name("Bob", "", "GB"))

    @Test
    fun constructorTest() {
        val value = 50
        val lender = a.party
        val borrower = b.party
        val uid = UniqueIdentifier()
        val st = SanctionableIOUState(value, lender, borrower, uid)
        Assert.assertTrue(st is ContractState)
        Assert.assertTrue(st is LinearState)
        Assert.assertEquals(value, st.value)
        Assert.assertEquals(lender, st.lender)
        Assert.assertEquals(borrower, st.borrower)
        Assert.assertEquals(uid, st.linearId)
        Assert.assertTrue(st.participants.contains(a.party))
        Assert.assertTrue(st.participants.contains(b.party))
    }
}
