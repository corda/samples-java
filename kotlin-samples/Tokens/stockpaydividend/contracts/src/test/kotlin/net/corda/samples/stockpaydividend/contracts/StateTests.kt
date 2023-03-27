package net.corda.samples.stockpaydividend.contracts

import net.corda.samples.stockpaydividend.states.StockState
import net.corda.testing.node.MockServices
import org.junit.Test


class StateTests {
    private val ledgerServices = MockServices()

    //sample State tests
    @Test
    @Throws(NoSuchFieldException::class)
    fun hasConstructionAreaFieldOfCorrectType() {
        // Does the message field exist?
        StockState::class.java.getDeclaredField("symbol")
        assert(StockState::class.java.getDeclaredField("symbol").type == String::class.java)
    }
}