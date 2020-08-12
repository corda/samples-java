package net.corda.examples.yo.states;

import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.examples.yo.contracts.YoFungibleTokenContract;
import org.jetbrains.annotations.NotNull;
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(YoFungibleTokenContract.class)
public class YoTokenFungible extends EvolvableTokenType {
    private final Party issuer;
    private String yo = "This is fungible yo token (Mimicking fungible onership such as stocks)";
    private final int fractionDigits=0;
    private final UniqueIdentifier id;

    @ConstructorForDeserialization
    public YoTokenFungible(Party issuer,UniqueIdentifier id, String yo) {
        this.issuer = issuer;
        this.id = id;
        this.yo = yo;
    }

    public YoTokenFungible(Party issuer) {
        this.issuer = issuer;
        this.id = new UniqueIdentifier();
    }

    public Party getIssuer() {
        return issuer;
    }

    public String getYo() {
        return yo;
    }

    public UniqueIdentifier getId() {
        return id;
    }

    @Override
    public int getFractionDigits() {
        return this.fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return Arrays.asList(issuer);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.id;
    }

    /* This method returns a TokenPointer by using the linear Id of the evolvable state */
    public TokenPointer<YoTokenFungible> toPointer(){
        LinearPointer<YoTokenFungible> linearPointer = new LinearPointer<>(this.id, YoTokenFungible.class);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }
}
