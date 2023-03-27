package net.corda.samples.avatar.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.samples.avatar.contracts.AvatarContract;
import net.corda.samples.avatar.contracts.ExpiryContract;
import net.corda.samples.avatar.states.Avatar;
import net.corda.samples.avatar.states.Expiry;
import net.corda.core.CordaRuntimeException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

@InitiatingFlow
@StartableByRPC
public class TransferAvatarFlow extends FlowLogic<SignedTransaction> {

    private final String avatarId;
    private final String buyer;

    public TransferAvatarFlow(String avatarId, String buyer) {
        this.avatarId = avatarId;
        this.buyer = buyer;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {

        Party buyerParty = getServiceHub().getIdentityService().partiesFromName(buyer, true).iterator().next();

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //get avatar from db
        Vault.Page<Avatar> avatarPage = getServiceHub().getVaultService().queryBy(Avatar.class);
        StateAndRef<Avatar> avatarStateAndRef = avatarPage.getStates().stream().filter(i ->
                i.getState().getData().getAvatarId().equalsIgnoreCase(avatarId)).findAny().orElseThrow(() ->
                new CordaRuntimeException("No avatar found with avatar id as : " + avatarId));;

        //get expiry from db
        Vault.Page<Expiry> expiryPage = getServiceHub().getVaultService().queryBy(Expiry.class);
        StateAndRef<Expiry> expiryStateAndRef = expiryPage.getStates().stream().filter(i ->
                i.getState().getData().getAvatarId().equalsIgnoreCase(avatarId)).findAny().orElseThrow(() ->
                new CordaRuntimeException("No expiry found with avatar id as " + avatarId));

        //change owner
        Avatar avatar = new Avatar(buyerParty, avatarId);
        Expiry expiry = new Expiry(expiryStateAndRef.getState().getData().getExpiry(), avatarId, buyerParty);

        //consume existing states, encumbering states will trigger the expiry contract to run
        TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                .addInputState(avatarStateAndRef)
                .addInputState(expiryStateAndRef)
                .addOutputState(avatar, AvatarContract.AVATAR_CONTRACT_ID, notary, 1)
                .addOutputState(expiry, ExpiryContract.EXPIRY_CONTRACT_ID, notary, 0)
                .addCommand(new AvatarContract.Commands.Transfer(), Arrays.asList(buyerParty.getOwningKey(),
                        avatarStateAndRef.getState().getData().getOwner().getOwningKey()))
                .addCommand(new ExpiryContract.Commands.Pass(), Arrays.asList(buyerParty.getOwningKey(),
                        expiryStateAndRef.getState().getData().getOwner().getOwningKey()))
                .setTimeWindow(Instant.now(), Duration.ofSeconds(10));

        transactionBuilder.verify(getServiceHub());

        SignedTransaction partiallySignedTx = getServiceHub().signInitialTransaction(transactionBuilder);
        FlowSession buyerSession = initiateFlow(buyerParty);

        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partiallySignedTx, Collections.singletonList(buyerSession)));

        return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(buyerSession)));
    }
}
