package net.corda.samples.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.bikemarket.states.WheelsTokenState

// *********
// * Flows *
// *********
@StartableByRPC
class CreateWheelToken(private val wheelsSerial: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        //Create non-fungible frame token
        val uuid = UniqueIdentifier()
        val wheel = WheelsTokenState(ourIdentity,uuid,0, wheelsSerial)

        //warp it with transaction state specifying the notary
        val transactionState = wheel withNotary notary!!

        subFlow(CreateEvolvableTokens(transactionState))

        return "\nCreated a wheel token for bike wheels. (Serial #" + this.wheelsSerial + ")."

    }
}
