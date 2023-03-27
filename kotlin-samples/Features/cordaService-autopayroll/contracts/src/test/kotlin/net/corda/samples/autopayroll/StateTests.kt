package net.corda.samples.autopayroll

import net.corda.samples.autopayroll.states.MoneyState
import net.corda.testing.node.MockServices
import org.junit.Assert
import org.junit.Test

class StateTests {
    private val ledgerServices = MockServices()

    @Test
    fun hasFieldOfCorrectType() {
        // Does the message field exist?
        MoneyState::class.java.getDeclaredField("amount")
        // Is the message field of the correct type?
        Assert.assertEquals(MoneyState::class.java.getDeclaredField("amount").type, Int::class.java)
    }
}