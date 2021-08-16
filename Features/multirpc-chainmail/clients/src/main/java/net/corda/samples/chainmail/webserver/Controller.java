package net.corda.samples.chainmail.webserver;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.corda.core.contracts.StateAndRef;
import net.corda.samples.chainmail.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;

import net.corda.samples.chainmail.states.MessageState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

class Event {
    private String sender;
    private String message;
    protected Event(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }
}

/**
 * Define your API endpoints here.
 *
 * Note we allow all origins for convenience, this is NOT a good production practice.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
@CrossOrigin(origins = "*")
public class Controller {
    private final CordaRPCOps proxy;
    private final String me;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private Party getPartyFromNodeInfo(NodeInfo nodeInfo) {
        Party target = nodeInfo.getLegalIdentities().get(0);
        return target;
    }

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = rpc.getUsername();
    }

    @RequestMapping(value = "/messages", method = RequestMethod.POST)
    public ResponseEntity<String> checkMessages(@RequestBody String payload) {

        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
        String requestingNode = convertedObject.get("requestingNode").getAsString();
        JsonObject resp = new JsonObject();

        try {
            MessagesInfo output = proxy.startTrackedFlowDynamic(GetMessagesForNode.class, me).getReturnValue().get();
            resp.addProperty("requestingNode", output.getRequestingNode());

            Collection messages = new ArrayList();

            for(StateAndRef<MessageState> messageState: output.getMessageStates()) {
                String sender = messageState.getState().getData().getSender().getName().getOrganisation();
                String message = messageState.getState().getData().getMessage().toString();
                messages.add(new Event(sender, message));
            }
            Gson gson = new Gson();
            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(messages));
        } catch (Exception e) {
            System.err.println(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @RequestMapping(value = "/messages/sendmessage", method = RequestMethod.POST)
    public void sendMessage(@RequestBody String payload) {
        JsonObject convertedObject = new Gson().fromJson(payload, JsonElement.class).getAsJsonObject();
        String message = convertedObject.get("message").getAsString();

        proxy.startTrackedFlowDynamic(SendMessage.class, message).getReturnValue();
    }
}
