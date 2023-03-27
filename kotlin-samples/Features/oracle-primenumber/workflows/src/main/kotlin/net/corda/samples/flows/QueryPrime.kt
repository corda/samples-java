package net.corda.samples.oracle.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.utilities.unwrap

// Simple flows that requests the Nth prime number from the specified oracle.
@InitiatingFlow
class QueryPrime(val oracle: Party, val n: Int) : FlowLogic<Int>() {
    @Suspendable override fun call() = initiateFlow(oracle).sendAndReceive<Int>(n).unwrap { it }
}
