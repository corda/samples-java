package net.corda.samples.snl.oracle.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap
import net.corda.samples.snl.diceservice.DiceRollService


@InitiatingFlow
class DiceRollerFlow(private val player: String, private val oracle: Party) : FlowLogic<Int?>() {
    @Suspendable override fun call() = initiateFlow(oracle).sendAndReceive(Int::class.java, player).unwrap { it }
}


@InitiatedBy(DiceRollerFlow::class)
class DiceRollerFlowHandler(private val requestSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Unit {
        val player = requestSession.receive(String::class.java).unwrap{ it -> it }
        val service: DiceRollService = serviceHub.cordaService(DiceRollService::class.java)
        val roll: Int = service.diceRoll(player)
        requestSession.send(roll)
        return
    }
}
