package net.corda.samples.chainmail.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.serialization.CordaSerializable;
import net.corda.samples.chainmail.states.MessageState;

import java.util.List;

@InitiatingFlow
@StartableByRPC
public class GetMessagesForNode extends FlowLogic<MessagesInfo> {
    private final String requestingNode;

    public GetMessagesForNode(String requestingNode) {
        this.requestingNode = requestingNode;
    }

    @Override
    @Suspendable
    public MessagesInfo call() throws FlowException {
        List<StateAndRef<MessageState>> messageStates = getServiceHub().getVaultService().queryBy(MessageState.class).getStates();
        System.out.println(messageStates);
        MessagesInfo messagesInfo = new MessagesInfo(
                this.requestingNode,
                messageStates
        );
        return messagesInfo;
    }
}
