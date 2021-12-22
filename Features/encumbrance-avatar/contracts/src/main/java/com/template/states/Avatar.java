package com.template.states;

import com.google.common.collect.ImmutableList;
import com.template.contracts.AvatarContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@BelongsToContract(AvatarContract.class)
public class Avatar implements ContractState {
    private final AbstractParty owner;
    private final String avatarId;

    public Avatar(AbstractParty owner, String avatarId) {
        this.owner = owner;
        this.avatarId = avatarId;
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public String getAvatarId() {
        return avatarId;
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
        Avatar avatar = (Avatar) o;
        return avatarId.equals(avatar.avatarId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avatarId);
    }
}
