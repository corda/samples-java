package net.corda.samples.observable.contracts

import net.corda.core.identity.Party
import net.corda.samples.observable.states.HighlyRegulatedState
import org.junit.Test

class StateTests {
    @Test
    @Throws(NoSuchFieldException::class)
    fun hasFieldOfCorrectType() {
        // Does the message field exist?
        HighlyRegulatedState::class.java.getDeclaredField("buyer")
        assert(HighlyRegulatedState::class.java.getDeclaredField("buyer").type == Party::class.java)
    }
}