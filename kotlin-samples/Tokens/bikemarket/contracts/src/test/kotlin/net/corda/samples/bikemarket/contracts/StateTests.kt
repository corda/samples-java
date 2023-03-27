package net.corda.samples.bikemarket.contracts

import net.corda.samples.bikemarket.states.WheelsTokenState
import net.corda.testing.node.MockServices
import org.junit.Test

class StateTests {
    private val ledgerServices = MockServices()

    //sample State tests
    @Test
    fun hasSerialNumFieldOfCorrectType() {
        // Does the message field exist?
        WheelsTokenState::class.java.getDeclaredField("serialNum")
        // Is the message field of the correct type?
        assert(WheelsTokenState::class.java.getDeclaredField("serialNum").type == String::class.java)
    }
}