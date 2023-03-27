package net.corda.samples.dollartohousetoken.contracts

import net.corda.samples.dollartohousetoken.states.HouseState
import net.corda.testing.node.MockServices
import org.junit.Test


class StateTests {
    private val ledgerServices = MockServices()

    //sample State tests
    @Test
    @Throws(NoSuchFieldException::class)
    fun hasConstructionAreaFieldOfCorrectType() {
        // Does the message field exist?
        HouseState::class.java.getDeclaredField("constructionArea")
        assert(HouseState::class.java.getDeclaredField("constructionArea").type == String::class.java)
    }
}