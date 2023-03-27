package net.corda.samples.snl.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.NodeInfo
import java.util.stream.Collectors


@StartableByRPC
@InitiatingFlow
class CreateAndShareAccountFlow(private val accountName: String) : FlowLogic<String?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {

        //Call inbuilt CreateAccount flow to create the AccountInfo object
        //notary
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val oracle = serviceHub.networkMapCache
                .getNodeByLegalName(CordaX500Name.parse("O=Oracle,L=Mumbai,C=IN"))!!.legalIdentities[0]
        val parties = serviceHub.networkMapCache.allNodes.stream()
                .map { nodeInfo: NodeInfo -> nodeInfo.legalIdentities[0] }
                .collect(Collectors.toList())
        parties.remove(ourIdentity)
        parties.remove(notary)
        parties.remove(oracle)

        //Share this AccountInfo object with the parties who want to transact with this account
        subFlow(ShareAccountInfo(subFlow(CreateAccount(accountName)), parties))
        return "" + accountName + "has been created and shared to " + parties + "."
    }
}
