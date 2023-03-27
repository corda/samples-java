package net.corda.samples.auction.contracts

import net.corda.samples.auction.states.AuctionState
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class StateTests {

    @Test
    fun hasFieldOfCorrectType() {
        // Does the amount field exist?
        AuctionState::class.java.getDeclaredField("auctionId")
        // Is the amount field of the correct type?
        assertEquals(AuctionState::class.java.getDeclaredField("auctionId").type, UUID::class.java)
    }
}