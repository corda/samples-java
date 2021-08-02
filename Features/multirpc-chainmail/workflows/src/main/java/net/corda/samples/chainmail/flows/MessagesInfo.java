package net.corda.samples.chainmail.flows;

import net.corda.core.contracts.StateAndRef;
import net.corda.core.serialization.CordaSerializable;
import net.corda.samples.chainmail.states.MessageState;

import java.util.List;

@CordaSerializable
public class MessagesInfo {
    private String requestingNode;
    private List<StateAndRef<MessageState>> messageStates;

    public MessagesInfo(String requestingNode, List<StateAndRef<MessageState>> messages) {
        this.requestingNode = requestingNode;
        this.messageStates = messages;
    }

    public String getRequestingNode() {
        return requestingNode;
    }

    public void setRequestingNode(String requestingNode) {
        this.requestingNode = requestingNode;
    }

    public List<StateAndRef<MessageState>> getMessageStates() {
        return messageStates;
    }

//    public void setMessageStates(List<StateAndRef<MessageState>> messageStates) {
//        this.messageStates = messageStates;
//    }

}
