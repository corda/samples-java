package net.corda.samples.example.contracts

import net.corda.samples.example.states.IOUState
import net.corda.testing.node.MockServices
import org.junit.Test

class StateTests {
    private val ledgerServices = MockServices()
    @Test
    @Throws(NoSuchFieldException::class)
    fun hasAmountFieldOfCorrectType() {
        // Does the message field exist?
        IOUState::class.java.getDeclaredField("value")
        assert(IOUState::class.java.getDeclaredField("value").getType() == Int::class.javaPrimitiveType)
    }
}