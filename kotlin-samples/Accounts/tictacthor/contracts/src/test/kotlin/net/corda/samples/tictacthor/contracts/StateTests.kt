package net.corda.samples.tictacthor.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.samples.tictacthor.states.BoardState
import net.corda.testing.node.MockServices
import org.junit.Assert
import org.junit.Test

class StateTests {
    private val ledgerServices = MockServices()

    @Test
    fun hasFieldOfCorrectType() {
        // Does the message field exist?
        BoardState::class.java.getDeclaredField("playerO")
        // Is the message field of the correct type?
        Assert.assertEquals(BoardState::class.java.getDeclaredField("playerO").type, UniqueIdentifier::class.java)
    }
}