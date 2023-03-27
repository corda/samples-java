package net.corda.samples.supplychain.states;

import net.corda.samples.supplychain.contracts.InvoiceStateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// *********
// * State *
// *********
@BelongsToContract(InvoiceStateContract.class)
public class InvoiceState implements ContractState {

    private int amount;
    private AnonymousParty sender;
    private AnonymousParty recipient;
    private UUID invoiceID;
    private List<AbstractParty> participants;

    public InvoiceState(int amount, AnonymousParty sender, AnonymousParty recipient, UUID invoiceID) {
        this.amount = amount;
        this.sender = sender;
        this.recipient = recipient;
        this.invoiceID = invoiceID;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(recipient);
        participants.add(sender);
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public AnonymousParty getSender() {
        return sender;
    }

    public void setSender(AnonymousParty sender) {
        this.sender = sender;
    }

    public AnonymousParty getRecipient() {
        return recipient;
    }

    public void setRecipient(AnonymousParty recipient) {
        this.recipient = recipient;
    }

    public UUID getInvoiceID() {
        return invoiceID;
    }

    public void setInvoiceID(UUID invoiceID) {
        this.invoiceID = invoiceID;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }
}