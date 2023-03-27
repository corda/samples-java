package net.corda.samples.snl.states


import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.samples.snl.contracts.BoardConfigContract


@BelongsToContract(BoardConfigContract::class)
class BoardConfig(val ladderPositions: Map<Int, Int>?,
                  val snakePositions: Map<Int, Int>?,
                  override val participants: List<AbstractParty>) : ContractState
