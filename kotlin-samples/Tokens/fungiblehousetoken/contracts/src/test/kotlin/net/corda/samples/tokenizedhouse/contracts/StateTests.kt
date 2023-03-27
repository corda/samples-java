package net.corda.samples.tokenizedhouse.contracts

import net.corda.samples.tokenizedhouse.states.FungibleHouseTokenState
import net.corda.testing.node.MockServices
import org.junit.Test


class StateTests {
    private val ledgerServices = MockServices()

    //sample State tests
    @Test
    @Throws(NoSuchFieldException::class)
    fun hasConstructionAreaFieldOfCorrectType() {
        // Does the message field exist?
        FungibleHouseTokenState::class.java.getDeclaredField("symbol")
        assert(FungibleHouseTokenState::class.java.getDeclaredField("symbol").type == String::class.java)
    }
}