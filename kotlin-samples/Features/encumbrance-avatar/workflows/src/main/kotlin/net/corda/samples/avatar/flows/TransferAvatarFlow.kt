package net.corda.samples.avatar.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.CordaRuntimeException
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.avatar.contracts.AvatarContract
import net.corda.samples.avatar.contracts.ExpiryContract
import net.corda.samples.avatar.states.Avatar
import net.corda.samples.avatar.states.Expiry
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Predicate
import java.util.function.Supplier


@InitiatingFlow
@StartableByRPC
class TransferAvatar(private val avatarId: String, private val buyer: String) :
    FlowLogic<SignedTransaction?>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val buyerParty = serviceHub.identityService.partiesFromName(buyer, true).iterator().next()
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        //get avatar from db
        val avatarPage: Vault.Page<Avatar> = serviceHub.vaultService.queryBy(Avatar::class.java)
        val avatarStateAndRef: StateAndRef<Avatar> =
            avatarPage.states.stream().filter(Predicate<StateAndRef<Avatar>> { (state): StateAndRef<Avatar> ->
                state.data.avatarId.equals(this.avatarId, true)
            }).findAny()
                .orElseThrow(Supplier { CordaRuntimeException("No avatar found with avatar id as : $avatarId") })

        //get expiry from db
        val expiryPage: Vault.Page<Expiry> = serviceHub.vaultService.queryBy(Expiry::class.java)
        val expiryStateAndRef: StateAndRef<Expiry> =
            expiryPage.states.stream().filter(Predicate<StateAndRef<Expiry>> { (state): StateAndRef<Expiry> ->
                state.data.avatarId.equals(avatarId, true)
            }).findAny().orElseThrow(Supplier { CordaRuntimeException("No expiry found with avatar id as $avatarId") })

        //change owner
        val avatar = Avatar(buyerParty, avatarId)
        val expiry = Expiry(expiryStateAndRef.state.data.expiry, avatarId, buyerParty)

        //consume existing states, encumbering states will trigger the expiry contract to run
        val transactionBuilder = TransactionBuilder(notary)
            .addInputState(avatarStateAndRef)
            .addInputState(expiryStateAndRef)
            .addOutputState(avatar, AvatarContract.AVATAR_CONTRACT_ID, notary, 1)
            .addOutputState(expiry, ExpiryContract.EXPIRY_CONTRACT_ID, notary, 0)
            .addCommand(
                AvatarContract.Commands.Transfer(), Arrays.asList(
                    buyerParty.owningKey,
                    avatarStateAndRef.state.data.owner.owningKey
                )
            )
            .addCommand(
                ExpiryContract.Commands.Pass(), Arrays.asList(
                    buyerParty.owningKey,
                    expiryStateAndRef.state.data.owner.owningKey
                )
            )
            .setTimeWindow(Instant.now(), Duration.ofSeconds(10))
        transactionBuilder.verify(serviceHub)
        val partiallySignedTx = serviceHub.signInitialTransaction(transactionBuilder)
        val buyerSession = initiateFlow(buyerParty)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partiallySignedTx, listOf(buyerSession)))
        return subFlow(FinalityFlow(fullySignedTx, listOf(buyerSession)))
    }
}

@InitiatedBy(TransferAvatar::class)
class TransferAvatarResponder(private val counterpartySession: FlowSession) : FlowLogic<SignedTransaction?>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                //Addition checks
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}