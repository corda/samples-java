package net.corda.samples.logging.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.logging.contracts.YoContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(YoContract.class)
public class YoState implements ContractState {
    private final Party origin;
    private final Party target;
    private final String yo;

    @ConstructorForDeserialization
    public YoState(Party origin, Party target, String yo) {
        this.origin = origin;
        this.target = target;
        this.yo = yo;
    }

    public YoState(Party origin, Party target) {
        this.origin = origin;
        this.target = target;
        this.yo = "Yo!";
    }

    public Party getOrigin() {
        return origin;
    }

    public Party getTarget() {
        return target;
    }

    public String getYo() {
        return yo;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(target);
    }

    @Override
    public String toString() {
        return origin.getName() + ": " + yo;
    }
}
