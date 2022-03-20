package net.corda.samples.statereissuance.states;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.samples.statereissuance.contracts.LandTitleContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// *********
// * State *
// *********
@BelongsToContract(LandTitleContract.class)
public class LandTitleState implements LinearState {

    /* This is the unique identifier of the property */
    private UniqueIdentifier plotIdentifier;
    private String dimensions;
    private String area;

    private Party owner;
    private Party issuer;

    /* Constructor of our Corda state */
    public LandTitleState(UniqueIdentifier plotIdentifier, String dimensions, String area, Party owner, Party issuer) {
        this.plotIdentifier = plotIdentifier;
        this.dimensions = dimensions;
        this.area = area;
        this.owner = owner;
        this.issuer = issuer;
    }

    //Getters

    public Party getOwner() {
        return owner;
    }

    public Party getIssuer() {
        return issuer;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getArea() {
        return area;
    }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer, owner);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return plotIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return plotIdentifier.equals(((LandTitleState) o).plotIdentifier);
    }

    @Override
    public int hashCode() {
        return plotIdentifier.hashCode();
    }
}