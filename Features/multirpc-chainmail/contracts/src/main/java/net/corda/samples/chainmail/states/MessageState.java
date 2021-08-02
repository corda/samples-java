package net.corda.samples.chainmail.states;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.chainmail.contracts.MessageContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;

import java.util.ArrayList;
import java.util.List;


// *********
// * State *
// *********
@BelongsToContract(MessageContract.class)
public class MessageState implements ContractState{

    private final Party sender;
    private final List<Party> recipients;
    private final String message;

    @ConstructorForDeserialization
    public MessageState(Party sender, List<Party> recipients, String message) {
        this.sender = sender;
        this.recipients = recipients;
        this.message = message;
    }

    public Party getSender() {
        return sender;
    }
    public String getMessage() {
        return message;
    }
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> allParties = new ArrayList<>(recipients);
        allParties.add(sender);
        return allParties;
    }
}
