package net.corda.samples.snl.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.samples.snl.oracle.flows.DiceRollerFlow

@StartableByRPC
class QueryOracleForDiceRollFlow(private val player: String) : FlowLogic<Int>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): Int {
        val oracle = serviceHub.networkMapCache
                .getNodeByLegalName(CordaX500Name.parse("O=Oracle,L=Mumbai,C=IN"))!!.legalIdentities[0]
        return subFlow(DiceRollerFlow(player, oracle))!!
    }
}