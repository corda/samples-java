package net.corda.samples.contractsdk.states;

import com.r3.corda.lib.contracts.contractsdk.verifiers.StateWithRoles;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.contractsdk.contracts.RecordPlayerContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;


// *********
// * State *
// *********
@BelongsToContract(RecordPlayerContract.class)
public class RecordPlayerState implements ContractState, LinearState, StateWithRoles {

    // we'll assume some basic stats about this record player
    private Needle needle; // enum describing the needle type or damage
    private int magneticStrength; // assume 100 gauss about the strength of a refrigerator magnet
    private int coilTurns; // typical number of turns in the coils of a record player cartridge
    private int amplifierSNR; // signal to noise ratio on the amplifier, 10,000 is pretty good.
    private final UniqueIdentifier uid; // unique id for this rare record player.

    private int songsPlayed; // assume a new player has not played any tracks and it's in mint condition.
    private String manufacturerNotes;

    private Party manufacturer;
    private Party dealer;

    /* Constructor */
    @ConstructorForDeserialization
    public RecordPlayerState(Party manufacturer, Party dealer, Needle needle, int magneticStrength, int coilTurns, int amplifierSNR, int songsPlayed, UniqueIdentifier uid) {

        if (songsPlayed < 0) {
            throw new IllegalArgumentException("Invalid songs played");
        }

        if (magneticStrength < 0) {
            throw new IllegalArgumentException("Invalid magnetic strength.");
        }

        this.manufacturer = manufacturer;
        this.dealer = dealer;

        this.needle = needle;
        this.magneticStrength = magneticStrength;
        this.coilTurns = coilTurns;
        this.amplifierSNR = amplifierSNR;
        this.songsPlayed = songsPlayed;

        this.uid = uid;
    }

    /* Constructor for a CordaGraf with default */
    public RecordPlayerState(Party manufacturer, Party dealer, Needle needle) {
        this(manufacturer, dealer,  needle, 100, 700, 10000, 0, new UniqueIdentifier());
    }

    public Needle getNeedle() {
        return needle;
    }

    public void setNeedle(Needle needle) {
        this.needle = needle;
    }

    public int getMagneticStrength() {
        return magneticStrength;
    }

    public void setMagneticStrength(int magneticStrength) {
        this.magneticStrength = magneticStrength;
    }

    public int getCoilTurns() {
        return coilTurns;
    }

    public void setCoilTurns(int coilTurns) {
        this.coilTurns = coilTurns;
    }

    public int getAmplifierSNR() {
        return amplifierSNR;
    }

    public void setAmplifierSNR(int amplifierSNR) {
        this.amplifierSNR = amplifierSNR;
    }

    public UniqueIdentifier getUid() {
        return uid;
    }

    public int getSongsPlayed() {
        return songsPlayed;
    }

    public void setSongsPlayed(int songsPlayed) {
        this.songsPlayed = songsPlayed;
    }

    public Party getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Party manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Party getDealer() {
        return dealer;
    }

    public void setOwner(Party dealer) {
        this.dealer = dealer;
    }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(manufacturer, dealer);
    }


    public RecordPlayerState update(Needle needle, int magneticStrength, int coilTurns, int amplifierSNR, int songsPlayed) {
        // take our params and apply them to the state object
        return new RecordPlayerState(manufacturer, dealer, needle, magneticStrength, coilTurns, amplifierSNR, songsPlayed, uid);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.getUid();
    }

    @NotNull
    @Override
    public Party getParty(@NotNull String role) {
        Party ret;

        role = role.toLowerCase();

        switch(role) {
            case "manufacturer" :
                ret = getManufacturer();
                break;

            case "dealer" :
                ret = getDealer();
                break; // optional

            default :
                ret =  getManufacturer();
                break;
        }
        return ret;
    }
}
