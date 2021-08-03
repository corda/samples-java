package net.corda.samples.chainmail.webserver;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.corda.core.contracts.StateAndRef;
import net.corda.samples.chainmail.flows.*;
import net.corda.core.identity.CordaX500Name;
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

import java.lang.reflect.Array;
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
    private final CordaX500Name me;
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
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    @RequestMapping(value = "/messages", method = RequestMethod.POST)
    public ResponseEntity<String> checkMessages(@RequestBody String payload) {

        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
        String requestingNode = convertedObject.get("requestingNode").getAsString();

        JsonObject resp = new JsonObject();

        try {
            MessagesInfo output = proxy.startTrackedFlowDynamic(GetMessagesForNode.class, requestingNode).getReturnValue().get();
            System.out.println(output.getRequestingNode());
            resp.addProperty("requestingNode", output.getRequestingNode());

//            LinkedHashMap<String, String> messages = new LinkedHashMap<String, String>();
            ArrayList<HashMap> messages = new ArrayList();
            Collection collection = new ArrayList();

            for(StateAndRef<MessageState> messageState: output.getMessageStates()) {
                String sender = messageState.getState().getData().getSender().getName().getOrganisation();
                String message = messageState.getState().getData().getMessage().toString();
                System.out.println("Sender: " + sender + " Message: " + message);
//                HashMap<String, String> senderMessage = new HashMap<>();
//                senderMessage.put("sender", sender);
//                senderMessage.put("message", message);
//                messages.add(senderMessage);

                collection.add(new Event(sender, message));
                System.out.println(collection);
//                System.out.println(messages);
            }
            System.out.println("ADDING PROPERTY");
//            resp.addProperty("messages", new Gson().toJson(messages, ArrayList.class));
//            resp.addProperty("messages", collection);
//            resp.addProperty("messages", String.valueOf(messages));
            System.out.println(resp);

            Gson gson = new Gson();
//            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(gson.toJson(collection));
        } catch (Exception e) {
            System.err.println(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @RequestMapping(value = "/messages/sendmessage", method = RequestMethod.POST)
    public void sendMessage(@RequestBody String payload) {
        System.out.println("ATTEMPTING MESSAGE SEND");
        JsonObject convertedObject = new Gson().fromJson(payload, JsonElement.class).getAsJsonObject();
        String message = convertedObject.get("message").getAsString();
        System.out.println("SENDING: " + message);

        proxy.startTrackedFlowDynamic(SendMessage.class, message).getReturnValue();
    }
}
