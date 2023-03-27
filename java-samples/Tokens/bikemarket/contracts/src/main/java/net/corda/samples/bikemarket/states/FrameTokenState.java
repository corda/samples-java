package net.corda.samples.bikemarket.states;

import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.LinearPointer;
import net.corda.samples.bikemarket.contracts.FrameContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.List;
@BelongsToContract(FrameContract.class)
public class FrameTokenState extends EvolvableTokenType {


    private final Party maintainer;
    private final UniqueIdentifier uniqueIdentifier;
    private final int fractionDigits;
    private final String serialNum;

    public FrameTokenState(Party maintainer, UniqueIdentifier uniqueIdentifier, int fractionDigits, String serialNum) {
        this.maintainer = maintainer;
        this.uniqueIdentifier = uniqueIdentifier;
        this.fractionDigits = fractionDigits;
        this.serialNum = serialNum;
    }

    public String getserialNum() {
        return serialNum;
    }


    public Party getIssuer() {
        return maintainer;
    }

    @Override
    public int getFractionDigits() {
        return this.fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return ImmutableList.of(maintainer);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.uniqueIdentifier;
    }

    /* This method returns a TokenPointer by using the linear Id of the evolvable state */
    public TokenPointer<FrameTokenState> toPointer(){
        LinearPointer<FrameTokenState> linearPointer = new LinearPointer<>(uniqueIdentifier, FrameTokenState.class);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }
}
