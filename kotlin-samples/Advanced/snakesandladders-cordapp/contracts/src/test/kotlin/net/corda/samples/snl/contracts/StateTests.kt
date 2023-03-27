package net.corda.samples.snl.contracts

import net.corda.core.contracts.UniqueIdentifier
import net.corda.samples.snl.states.GameBoard
import net.corda.testing.node.MockServices
import org.junit.Test


class StateTests {
    private val ledgerServices = MockServices()
    @Test
    @Throws(NoSuchFieldException::class)
    fun hasFieldOfCorrectType() {
        // Does the message field exist?
        GameBoard::class.java.getDeclaredField("linearId")
        assert(GameBoard::class.java.getDeclaredField("linearId").type == UniqueIdentifier::class.java)
    }
}