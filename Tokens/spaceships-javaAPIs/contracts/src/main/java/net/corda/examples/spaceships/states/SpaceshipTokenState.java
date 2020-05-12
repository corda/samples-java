package net.corda.examples.spaceships.states;

import com.r3.corda.lib.tokens.contracts.types.TokenType;
import net.corda.core.contracts.BelongsToContract;
import net.corda.examples.spaceships.contracts.SpaceshipTokenContract;
import org.jetbrains.annotations.NotNull;

@BelongsToContract(SpaceshipTokenContract.class)
public class SpaceshipTokenState extends TokenType {
    private final String model;
    private final String planetOfOrigin;
    private final int seatingCapacity;

    public SpaceshipTokenState(@NotNull String tokenIdentifier, int fractionDigits, String model, String planetOfOrigin, int seatingCapacity) {
        super(tokenIdentifier, fractionDigits);
        this.model = model;
        this.planetOfOrigin = planetOfOrigin;
        this.seatingCapacity = seatingCapacity;
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
}
