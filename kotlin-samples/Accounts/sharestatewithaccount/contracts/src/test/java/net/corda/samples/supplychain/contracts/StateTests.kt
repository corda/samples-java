package net.corda.samples.supplychain.contracts

import groovy.util.GroovyTestCase.assertEquals
import net.corda.samples.supplychain.states.InvoiceState
import net.corda.testing.node.MockServices
import org.junit.Test

class StateTests {
    private val ledgerServices = MockServices()

    @Test
    fun hasMessageFieldOfCorrectType() {
        // Does the message field exist?
        InvoiceState::class.java.getDeclaredField("amount")
        // Is the message field of the correct type?
        assertEquals(InvoiceState::class.java.getDeclaredField("amount").type, Int::class.java)
    }
}