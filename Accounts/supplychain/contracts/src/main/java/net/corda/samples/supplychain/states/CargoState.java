package net.corda.samples.supplychain.states;

import net.corda.samples.supplychain.contracts.CargoStateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;

import java.util.ArrayList;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(CargoStateContract.class)
public class CargoState implements ContractState {

    private AnonymousParty pickUpFrom;
    private AnonymousParty deliverTo;
    private String cargo;
    private AbstractParty shipper;
    private List<AbstractParty> participants;


    public CargoState(AnonymousParty pickUpFrom, AnonymousParty deliverTo, String cargo, AbstractParty shipper) {
        this.pickUpFrom = pickUpFrom;
        this.deliverTo = deliverTo;
        this.cargo = cargo;
        this.shipper = shipper;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(pickUpFrom);
        participants.add(deliverTo);
        participants.add(shipper);
    }

    public AnonymousParty getPickUpFrom() {
        return pickUpFrom;
    }

    public void setPickUpFrom(AnonymousParty pickUpFrom) {
        this.pickUpFrom = pickUpFrom;
    }

    public AnonymousParty getDeliverTo() {
        return deliverTo;
    }

    public void setDeliverTo(AnonymousParty deliverTo) {
        this.deliverTo = deliverTo;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public AbstractParty getShipper() {
        return shipper;
    }

    public void setShipper(AnonymousParty shipper) {
        this.shipper = shipper;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
}