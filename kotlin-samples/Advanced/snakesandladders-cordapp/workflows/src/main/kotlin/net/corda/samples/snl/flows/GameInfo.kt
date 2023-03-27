package net.corda.samples.snl.flows

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
class GameInfo(val linearId: UniqueIdentifier, val player1: String, val player2: String, val currentPlayer: String, val player1Pos: Int,
               val player2Pos: Int, val winner: String, val lastRoll: Int)