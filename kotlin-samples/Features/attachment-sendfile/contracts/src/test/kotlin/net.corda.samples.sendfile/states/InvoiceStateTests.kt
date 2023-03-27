package net.corda.samples.sendfile.contracts

import net.corda.core.identity.CordaX500Name
import net.corda.samples.sendfile.states.InvoiceState
import net.corda.testing.core.TestIdentity
import org.junit.Assert
import org.junit.Test

class InvoiceStateTests {
    private val a = TestIdentity(CordaX500Name("Alice", "", "GB")).party
    private val b = TestIdentity(CordaX500Name("Bob", "", "GB")).party
    private val STRINGID = "StringID that is unique"

    @Test
    fun constructorTest() {
        val (invoiceAttachmentID) = InvoiceState(STRINGID, listOf(a, b))
        Assert.assertEquals(STRINGID, invoiceAttachmentID)
    }
}
