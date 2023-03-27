package net.corda.samples.auction.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.workflows.asset.CashUtils
import net.corda.samples.auction.states.Asset
import net.corda.samples.auction.states.AuctionState
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class AuctionDvPFlow(private val auctionId: UUID,
                     private val payment: Amount<Currency>) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():SignedTransaction {
        // Query the vault to fetch a list of all AuctionState state, and filter the results based on the auctionId
        // to fetch the desired AuctionState state from the vault. This filtered state would be used as input to the
        // transaction.
        val auctionStateAndRefs = serviceHub.vaultService.queryBy<AuctionState>().states
        val inputStateAndRef = auctionStateAndRefs.filter {
            val auctionState = it.state.data
            auctionState.auctionId == this.auctionId
        }[0]
        val auctionState = inputStateAndRef.state.data

        // Create a QueryCriteria to query the Asset.
        // Resolve the linear pointer in previously filtered auctionState to fetch the assetState containing
        // the asset's unique id.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(inputStateAndRef.state.data.auctionItem!!.resolve(serviceHub).state.data.linearId.id),
                null,Vault.StateStatus.UNCONSUMED)

        // Use the vaultQuery with the previously created queryCriteria to fetch th assetState to be used as input
        // in the transaction.
        val assetStateAndRef = serviceHub.vaultService.queryBy<Asset>(queryCriteria).states[0]

        // Use the withNewOwner() of the Ownable state get the command and the output state to be used in the
        // transaction from ownership transfer of the asset.
        val commandAndState = assetStateAndRef.state.data.withNewOwner(auctionState.winner!!)

        // Create the transaction builder.

        // Obtain a reference from a notary we wish to use.
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        val txBuilderPre = TransactionBuilder(notary)

        // Generate Spend for the Cash. The CashUtils generateSpend method can be used to update the transaction
        // builder with the appropriate inputs and outputs corresponding to the cash spending. A new keypair is
        // generated to sign the transaction, so that the the change returned to the spender after the cash is spend
        // is untraceable.
        val txAndKeysPair = CashUtils.generateSpend(
                serviceHub,txBuilderPre,
                payment,ourIdentityAndCert,
                auctionState.auctioneer!!, emptySet())
        val txBuilder = txAndKeysPair.first

        txBuilder.addInputState(assetStateAndRef)
                .addOutputState(commandAndState.ownableState)
                .addCommand(commandAndState.command, listOf(auctionState.auctioneer!!.owningKey))

        // Verify the transaction
        txBuilder.verify(serviceHub)

        // Sign the transaction. The transaction should be sigend with the new keyPair generated for Cash spending
        // and the node's key.
        val keysToSign = txAndKeysPair.second.plus(ourIdentity.owningKey)
        val stx = serviceHub.signInitialTransaction(txBuilder,keysToSign)

        // Collect counterparty signature.
        val auctioneerFlow = initiateFlow(auctionState.auctioneer!!)
        val ftx = subFlow(CollectSignaturesFlow(stx, listOf(auctioneerFlow)))
        return subFlow(FinalityFlow(ftx,(auctioneerFlow)))
    }
}

@InitiatedBy(AuctionDvPFlow::class)
class AuctionDvPFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        })
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
