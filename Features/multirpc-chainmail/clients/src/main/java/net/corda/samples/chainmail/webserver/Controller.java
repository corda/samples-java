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

import java.util.*;


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
//    @RequestMapping(value = "/messages", method = RequestMethod.GET)
    public ResponseEntity<String> checkMessages(@RequestBody String payload) {

//    return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body("{\"requestingNode\":\"Alice\",\"messages\":[{\"sender\": \"Alice\", \"message\": \"CATS\"}]}");

        System.out.println("ENTERED /messages");
        System.out.println(payload);
//        JsonObject convertedObject = new Gson().fromJson(payload, JsonElement.class).getAsJsonObject();
        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
        System.out.println("OBJECT PARSED");
//        UniqueIdentifier gameId = UniqueIdentifier.Companion.fromString(convertedObject.get("gameId").getAsString());
//        UniqueIdentifier requestingNode = UniqueIdentifier.Companion.fromString(convertedObject.get("requestingNode").getAsString());:w
        System.out.println("Attempting String Conversion");
//        String requestingNode = convertedObject.getAsString();
        String requestingNode = convertedObject.get("requestingNode").getAsString();
//        System.out.println(requestingNode);
        // NOTE lowercase the name for easy retrieve
//        String playerName = "\"" + convertedObject.get("name").getAsString().toLowerCase().trim() + "\"";
        // optional param
//        Boolean sendEmail = convertedObject.get("sendEmail").getAsBoolean();
        JsonObject resp = new JsonObject();
//        resp.addProperty("requestingNode", output.getRequestingNode());
        System.out.println("TRYING");
        try {
            System.out.println("TRY ENTERED");
            System.out.println(requestingNode);
            // TODO , does this need to be a state?
//            MessagesInfo output = proxy.startTrackedFlowDynamic(GetMessagesForNode.class, requestingNode).getReturnValue().get();
            MessagesInfo output = proxy.startTrackedFlowDynamic(GetMessagesForNode.class, requestingNode).getReturnValue().get();
            System.out.println(output.getRequestingNode());
            resp.addProperty("requestingNode", output.getRequestingNode());
            System.out.println("RESP BEING PRINTED NEXT");
            System.out.println(resp);
            LinkedHashMap<String, String> messages = null;
            for(StateAndRef<MessageState> messageState: output.getMessageStates()) {
                String sender = messageState.getState().getData().getSender().getName().toString();
                String message = messageState.getState().getData().getMessage();
                messages.put(sender, message);
            }
            resp.addProperty("messages", new Gson().toJson(messages, LinkedHashMap.class));
            System.out.println(resp.toString());


//            ChainMailSessionState output = proxy.startTrackedFlowDynamic(CheckAssignedChainMailFlow.class, gameId).getReturnValue().get();
//            if (output.getAssignments().get(playerName) == null) {
//                resp.addProperty("target", "target not found in this game");
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//            }
//            List<String> playerNames = output.getPlayerNames();
//            List<String> playerEmails = output.getPlayerEmails();
//            String playerEmail = playerEmails.get(playerNames.indexOf(playerName));
//            String targetName = output.getAssignments().get(playerName).replace("\"", "");
//            resp.addProperty("target", targetName);
//            if (sendEmail) {
//                System.out.println("sending email to "+ playerEmail);
//                boolean b = sendEmail(playerEmail, craftNotice(playerName, targetName, gameId));
//
//                if (!b) {
//                    System.out.println("ERROR: Failed to send email.");
//                }
//            }

            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
        } catch (Exception e) {
            System.err.println(e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }



    @RequestMapping(value = "/messages/sendmessage", method = RequestMethod.POST)
    public void sendMessage(@RequestBody String payload) {
        JsonObject convertedObject = new Gson().fromJson(payload, JsonElement.class).getAsJsonObject();
        String message = convertedObject.getAsString();

        proxy.startTrackedFlowDynamic(SendMessage.class, message).getReturnValue();
    }

//    @RequestMapping(value = "/node", method = RequestMethod.GET)
//    private ResponseEntity<String> returnName() {
//        JsonObject resp = new JsonObject();
//        resp.addProperty("name", me.toString());
//        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//    }
//
//    @RequestMapping(value = "/games/check", method = RequestMethod.POST)
//    public ResponseEntity<String> checkGame(@RequestBody String payload) {
//        System.out.println(payload);
//        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
//        UniqueIdentifier gameId = UniqueIdentifier.Companion.fromString(convertedObject.get("gameId").getAsString());
//        // NOTE lowercase the name for easy retrieve
//        String playerName = "\"" + convertedObject.get("name").getAsString().toLowerCase().trim() + "\"";
//        // optional param
//        Boolean sendEmail = convertedObject.get("sendEmail").getAsBoolean();
//        JsonObject resp = new JsonObject();
//        try {
//            ChainMailSessionState output = proxy.startTrackedFlowDynamic(CheckAssignedChainMailFlow.class, gameId).getReturnValue().get();
//            if (output.getAssignments().get(playerName) == null) {
//                resp.addProperty("target", "target not found in this game");
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//            }
//            List<String> playerNames = output.getPlayerNames();
//            List<String> playerEmails = output.getPlayerEmails();
//            String playerEmail = playerEmails.get(playerNames.indexOf(playerName));
//            String targetName = output.getAssignments().get(playerName).replace("\"", "");
//            resp.addProperty("target", targetName);
//            if (sendEmail) {
//                System.out.println("sending email to "+ playerEmail);
//                boolean b = sendEmail(playerEmail, craftNotice(playerName, targetName, gameId));
//
//                if (!b) {
//                    System.out.println("ERROR: Failed to send email.");
//                }
//            }
//
//            return ResponseEntity.status(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }
//
//    @RequestMapping(value = "/games", method = RequestMethod.POST)
//    public ResponseEntity<String> createGame(@RequestBody String payload) {
//
//        System.out.println(payload);
//        JsonObject convertedObject = new Gson().fromJson(payload, JsonObject.class);
//
//        JsonArray pNames = convertedObject.getAsJsonArray("playerNames");
//        JsonArray pMails = convertedObject.getAsJsonArray("playerEmails");
//
//        // optional param
//        Boolean sendEmail = convertedObject.get("sendEmail").getAsBoolean();
//
//        List<String> playerNames = new ArrayList<>();
//        List<String> playerEmails = new ArrayList<>();
//
//        // NOTE: we lowercase all names internally for clarity
//        for (JsonElement jo : pNames) {
//            String newName = jo.toString().toLowerCase();
//
//            if (!playerNames.contains(newName)) {
//                playerNames.add(newName);
//            }
//        }
//        for (JsonElement jo : pMails) {
//            playerEmails.add(jo.toString());
//        }
//
//        try {
//            Party elf = getPartyFromNodeInfo(proxy.networkMapSnapshot().get(2));
//
//            // run the flow to create our game
//            ChainMailSessionState output = proxy.startTrackedFlowDynamic(CreateSantaSessionFlow.class, playerNames, playerEmails, elf).getReturnValue().get().getTx().outputsOfType(ChainMailSessionState.class).get(0);
//            UniqueIdentifier gameId = output.getLinearId();
//            LinkedHashMap<String, String> assignments = output.getAssignments();
//
//            System.out.println("Created Secret Santa Game ID# " + output.getLinearId());
//
//            // send email to each player with the assignments
//            for (String p: playerNames) {
//                String t = assignments.get(p).replace("\"", "");
//                String msg = craftNotice(p, t, gameId);
//                int ind = playerNames.indexOf(p);
//                String email = playerEmails.get(ind).replace("\"", "");
//
//                if (sendEmail) {
//                    boolean b = sendEmail(email, msg);
//
//                    if (!b) {
//                        System.out.println("ERROR: Failed to send email.");
//                    }
//                }
//            }
//
//            JsonObject resp = new JsonObject();
//            resp.addProperty("gameId", gameId.toString());
//            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(resp.toString());
//
//        } catch (Exception e) {
//            System.out.println("Exception : " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }
//
//
//    public String craftNotice(String p, String t, UniqueIdentifier gameId) {
//        return "Hello, " + p + "!" + "\n" + "Your super secret santa target for game # " + gameId.toString() + " is " + t + ".";
//    }

//    public boolean sendEmail(String recipient, String msg) {
//
//        // TODO replace the below line with your *REAL* api key!
////        final String SENDGRID_API_KEY = "SG.xxxxxx_xxxxxxxxxxxxxxx.xxxxxxxxxxxxxxxx_xxxxxxxxxxxxxxxxxx_xxxxxxx";
//        final String SENDGRID_API_KEY = "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";
//
//        if (!SENDGRID_API_KEY.equals("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")) {
//            System.out.println(SENDGRID_API_KEY);
//            System.out.println("You haven't added your sendgrid api key!");
//            return true;
//        }
//
//        // you'll need to specify your sendgrid verified sender identity for this to mail out.
////        Email from = new Email("test@example.com");
//        Email from = new Email("olliegilbey@gmail.com");
//        String subject = "Secret Assignment from the Elves";
//        Email to = new Email(recipient);
//        Content content = new Content("text/plain", msg);
//        Mail mail = new Mail(from, subject, to, content);
//
//        SendGrid sg = new SendGrid(SENDGRID_API_KEY);
//        Request request = new Request();
//
//        try {
//            request.setMethod(Method.POST);
//            request.setEndpoint("mail/send");
//            request.setBody(mail.build());
//            Response response = sg.api(request);
//            System.out.println(response.getStatusCode());
//            System.out.println(response.getBody());
//            System.out.println(response.getHeaders());
//            return true;
//        } catch (IOException ex) {
//            System.out.println(ex);
//            return false;
//        }
//    }
}
