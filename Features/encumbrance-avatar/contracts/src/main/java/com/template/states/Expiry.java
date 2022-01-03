package com.template.states;

import com.google.common.collect.ImmutableList;
import com.template.contracts.ExpiryContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

//Expiry represents an expiry date beyond which the avatar cannot be sold. This is the encumbrance state which encumbers
//the Avatar state.
@BelongsToContract(ExpiryContract.class)
public class Expiry implements ContractState {

    private final Instant expiry;
    private final String avatarId;
    private final AbstractParty owner;

    public Expiry(Instant expiry, String avatarId, AbstractParty owner) {
        this.expiry = expiry;
        this.avatarId = avatarId;
        this.owner = owner;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public AbstractParty getOwner() {
        return owner;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(owner);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expiry expiry1 = (Expiry) o;
        return expiry.equals(expiry1.expiry) && avatarId.equals(expiry1.avatarId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiry, avatarId);
    }
}
