package net.corda.samples.avatar.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.samples.avatar.contracts.AvatarContract;
import net.corda.samples.avatar.contracts.ExpiryContract;
import net.corda.samples.avatar.states.Avatar;
import net.corda.samples.avatar.states.Expiry;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@InitiatingFlow
@StartableByRPC
public class CreateAvatarFlow extends FlowLogic<SignedTransaction> {

    private final String avatarId;
    private final long expiryAfterMinutes;

    public CreateAvatarFlow(String avatarId, long expiryAfterMinutes) {
        this.avatarId = avatarId;
        if (expiryAfterMinutes <= 0)
            throw new IllegalArgumentException("please provide positive value for expireAfterMinutes");
        this.expiryAfterMinutes = expiryAfterMinutes;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        Avatar avatar = new Avatar(this.getOurIdentity(), avatarId);
        Expiry expiry = new Expiry(Instant.now().plus(expiryAfterMinutes, ChronoUnit.MINUTES), avatarId, avatar.getOwner());

        //add expiry and avatar as outputs by specifying encumbrance as index. add time window
        //encumbrance can be identified by the output index. expiry is at output index 1 so we add 1 as the encumbrance
        //value while adding avatar as an output state and vice versa.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(avatar, AvatarContract.AVATAR_CONTRACT_ID, notary, 1) //specify the encumbrance as the 3rd parameter
                .addOutputState(expiry, ExpiryContract.EXPIRY_CONTRACT_ID, notary, 0) //specify the encumbrance as the 3rd parameter
                .addCommand(new AvatarContract.Commands.Create(), avatar.getOwner().getOwningKey())
                .addCommand(new ExpiryContract.Commands.Create(), expiry.getOwner().getOwningKey())
                .setTimeWindow(Instant.now(), Duration.ofSeconds(10));

        txBuilder.verify(getServiceHub());

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

        return subFlow(new FinalityFlow(signedTransaction, Arrays.asList()));
    }
}
