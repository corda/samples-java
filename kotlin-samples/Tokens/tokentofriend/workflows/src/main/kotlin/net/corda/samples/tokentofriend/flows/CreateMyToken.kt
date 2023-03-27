package net.corda.samples.tokentofriend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.samples.tokentofriend.states.CustomTokenState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@StartableByRPC
class CreateMyToken(private val myEmail: String, private val recipients: String, private val msg:String) : FlowLogic<UniqueIdentifier>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():UniqueIdentifier {
        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        //Create non-fungible frame token
        val uuid = UniqueIdentifier()
        val tokenState = CustomTokenState(myEmail,recipients,msg ,ourIdentity,0,uuid)

        //warp it with transaction state specifying the notary
        val transactionState = tokenState withNotary notary!!

        //Token Creation done
        subFlow(CreateEvolvableTokens(transactionState))
        return uuid
    }
}