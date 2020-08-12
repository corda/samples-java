package net.corda.examples.yo.states;

import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import net.corda.examples.yo.contracts.YoContract;
import net.corda.examples.yo.contracts.YoNonFungibleTokenContract;
import org.jetbrains.annotations.NotNull;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(YoNonFungibleTokenContract.class)
public class YoTokenNonFungible extends EvolvableTokenType {
    private final Party issuer;
    private String yo = "This is a non-fungible yo token (Mimicking non-fungible asset such as asset ownership)";
    private final int fractionDigits=0;
    private final UniqueIdentifier id;


    @ConstructorForDeserialization
    public YoTokenNonFungible(Party issuer,UniqueIdentifier id, String yo) {
        this.issuer = issuer;
        this.id = id;
        this.yo = yo;
    }


    public YoTokenNonFungible(Party issuer) {
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
    public TokenPointer<YoTokenNonFungible> toPointer(){
        LinearPointer<YoTokenNonFungible> linearPointer = new LinearPointer<>(this.id, YoTokenNonFungible.class);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }
}
