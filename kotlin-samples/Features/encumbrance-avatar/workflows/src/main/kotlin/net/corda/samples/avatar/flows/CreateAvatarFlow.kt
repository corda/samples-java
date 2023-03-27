package net.corda.samples.avatar.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.samples.avatar.contracts.AvatarContract
import net.corda.samples.avatar.contracts.ExpiryContract
import net.corda.samples.avatar.states.Avatar
import net.corda.samples.avatar.states.Expiry
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow
@StartableByRPC
class CreateAvatar(private val avatarId: String, expiryAfterMinutes: Long) : FlowLogic<SignedTransaction?>() {
    private val expiryAfterMinutes: Long

    init {
        if (expiryAfterMinutes <= 0) throw IllegalArgumentException("please provide positive value for expireAfterMinutes")
        this.expiryAfterMinutes = expiryAfterMinutes
    }

    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities[0]
        val avatar = Avatar(ourIdentity, avatarId)
        val expiry = Expiry(
            Instant.now().plus(expiryAfterMinutes, ChronoUnit.MINUTES), avatarId, avatar.owner
        )

        //add expiry and avatar as outputs by specifying encumbrance as index. add time window
        //encumbrance can be identified by the output index. expiry is at output index 1 so we add 1 as the encumbrance
        //value while adding avatar as an output state and vice versa.
        val txBuilder: TransactionBuilder = TransactionBuilder(notary).addOutputState(
            avatar, AvatarContract.AVATAR_CONTRACT_ID, notary, 1
        ) //specify the encumbrance as the 3rd parameter
            .addOutputState(
                expiry, ExpiryContract.EXPIRY_CONTRACT_ID, notary, 0
            ) //specify the encumbrance as the 3rd parameter
            .addCommand(AvatarContract.Commands.Create(), avatar.owner.owningKey)
            .addCommand(ExpiryContract.Commands.Create(), expiry.owner.owningKey)
            .setTimeWindow(Instant.now(), Duration.ofSeconds(10))
        txBuilder.verify(serviceHub)
        val signedTransaction = serviceHub.signInitialTransaction(txBuilder)
        return subFlow(FinalityFlow(signedTransaction, Arrays.asList()))
    }
}