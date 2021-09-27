package com.tutorial.states;

import com.tutorial.contracts.BasketOfAppleContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BelongsToContract(BasketOfAppleContract.class)
public class BasketOfApple implements ContractState {

    //Private Variables
    private String description; //Brand or type
    private Party farm; //Origin of the apple
    private Party owner; //The person who exchange the basket of apple with the stamp.
    private int weight;

    //ALL Corda State required parameter to indicate storing parties
    private List<AbstractParty> participants;

    //Constructors
    //Basket of Apple creation. Only farm name is stored.
    public BasketOfApple(String description, Party farm, int weight) {
        this.description = description;
        this.farm = farm;
        this.owner=farm;
        this.weight = weight;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(farm);
    }

    //Constructor for object creation during transaction
    @ConstructorForDeserialization
    public BasketOfApple(String description, Party farm, Party owner, int weight) {
        this.description = description;
        this.farm = farm;
        this.owner = owner;
        this.weight = weight;
        this.participants = new ArrayList<AbstractParty>();
        this.participants.add(farm);
        this.participants.add(owner);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }

    //getters
    public String getDescription() {
        return description;
    }

    public Party getFarm() {
        return farm;
    }

    public Party getOwner() {
        return owner;
    }

    public int getWeight() {
        return weight;
    }

    public BasketOfApple changeOwner(Party buyer){
        BasketOfApple newOwnerState = new BasketOfApple(this.description,this.farm,buyer,this.weight);
        return newOwnerState;
    }

}

//Advance version will fill in brand and type.