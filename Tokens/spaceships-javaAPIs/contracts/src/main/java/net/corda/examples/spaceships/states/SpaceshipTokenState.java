package net.corda.examples.spaceships.states;

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearPointer;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.Party;
import net.corda.examples.spaceships.contracts.SpaceshipTokenContract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@BelongsToContract(SpaceshipTokenContract.class)
public class SpaceshipTokenState extends EvolvableTokenType {
    private final Party manufacturer;
    private final String model;
    private final String planetOfOrigin;
    private final int seatingCapacity;
    private final UniqueIdentifier linearId;
    private final int fractionDigits;
    private final boolean fungible;
    private final Amount<TokenType> value; // price OR price/share in case of Fungible

    public SpaceshipTokenState(Party manufacturer, String model, String planetOfOrigin, int seatingCapacity, Amount<TokenType> value, boolean fungible) {
        this.manufacturer = manufacturer;
        this.linearId = new UniqueIdentifier();
        this.model = model;
        this.planetOfOrigin = planetOfOrigin;
        this.seatingCapacity = seatingCapacity;
        if (fungible) this.fractionDigits = 4;
        else this.fractionDigits = 0;
        this.value = value;
        this.fungible = fungible;
    }

    public String getModel() {
        return model;
    }

    public String getPlanetOfOrigin() {
        return planetOfOrigin;
    }

    public int getSeatingCapacity() {
        return seatingCapacity;
    }

    public Party getManufacturer() {
        return manufacturer;
    }

    public Amount<TokenType> getValue() {
        return value;
    }

    public boolean isFungible() {
        return fungible;
    }

    @Override
    public int getFractionDigits() {
        return fractionDigits;
    }

    @NotNull
    @Override
    public List<Party> getMaintainers() {
        return Collections.singletonList(manufacturer);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }



    /* This method returns a TokenPointer by using the linear Id of the evolvable state */
    public TokenPointer<SpaceshipTokenState> toPointer(){
        LinearPointer<SpaceshipTokenState> linearPointer = new LinearPointer<>(linearId, SpaceshipTokenState.class);
        return new TokenPointer<>(linearPointer, fractionDigits);
    }
}
