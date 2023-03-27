package net.corda.samples.businessmembership.contracts

import net.corda.core.identity.Party
import net.corda.samples.businessmembership.states.InsuranceState
import net.corda.testing.node.MockServices
import org.junit.Assert
import org.junit.Test

class StateTests {
    private val ledgerServices = MockServices()

    @Test
    fun hasFieldOfCorrectType() {
        // Does the message field exist?
        InsuranceState::class.java.getDeclaredField("insurer")
        // Is the message field of the correct type?
        Assert.assertEquals(InsuranceState::class.java.getDeclaredField("insurer").type, Party::class.java)
    }
}