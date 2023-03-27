package net.corda.samples.snl.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class StartGameFlow(private val player1: String, private val player2: String) : FlowLogic<String>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {
        subFlow(CreateBoardConfig.Initiator(player1, player2))
        return subFlow(CreateGameFlow.Initiator(player1, player2))
    }
}