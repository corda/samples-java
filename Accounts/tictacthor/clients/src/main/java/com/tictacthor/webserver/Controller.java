package com.tictacthor.webserver;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tictacthor.accountUtilities.CreateNewAccount;
import com.tictacthor.accountUtilities.ShareAccountTo;
import com.tictacthor.accountUtilities.myGame;
import com.tictacthor.flows.EndGameFlow;
import com.tictacthor.flows.StartGameFlow;
import com.tictacthor.flows.SubmitTurnFlow;
import com.tictacthor.states.BoardState;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import java.util.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/me",produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami(){
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    @PostMapping(value =  "createAccount/{acctName}")
    private ResponseEntity<String> createAccount(@PathVariable String acctName){
        try{
            String result = proxy.startTrackedFlowDynamic(CreateNewAccount.class,acctName).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Account "+acctName+" Created");

        }catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(e.getMessage());
        }
    }

    @PostMapping(value = "requestGameWith/{whoAmI}/{team}/{competeWith}")
    private ResponseEntity<String> requestGameWith(@PathVariable String whoAmI,@PathVariable String team, @PathVariable String competeWith){
        Set<Party> matchingPasties = proxy.partiesFromName(team,false);
        try{

            Iterator iter = matchingPasties.iterator();
            String result = proxy.startTrackedFlowDynamic(ShareAccountTo.class,whoAmI,iter.next()).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Game Request has Sent. When "+competeWith+" accepts your challenge, the game will start!");

        }catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PostMapping(value = "acceptGameInvite/{whoAmI}/{team}/{competeWith}")
    private ResponseEntity<String> acceptGameInvite(@PathVariable String whoAmI,@PathVariable String team, @PathVariable String competeWith){
        Set<Party> matchingPasties = proxy.partiesFromName(team,false);
        try{

            Iterator iter = matchingPasties.iterator();
            String result = proxy.startTrackedFlowDynamic(ShareAccountTo.class,whoAmI,iter.next()).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("I, "+whoAmI+" accepts "+competeWith+"'s challenge. Let's play!");

        }catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PostMapping(value = "startGameAndFirstMove/{whoAmI}/{competeWith}/{position}")
    private ResponseEntity<String> startGameAndFirstMove(@PathVariable String whoAmI,
                              @PathVariable String competeWith,
                              @PathVariable String position){
        int x  = -1;
        int y  = -1;
        int pos = Integer.parseInt(position);
        if(pos == 0) {
            x=0;y=0;
        }else if(pos == 1){
            x=0;y=1;
        }else if(pos == 2){
            x=0;y=2;
        }else if(pos == 3){
            x=1;y=0;
        }else if(pos == 4){
            x=1;y=1;
        }else if(pos == 5){
            x=1;y=2;
        }else if(pos == 6){
            x=2;y=0;
        }else if(pos == 7){
            x=2;y=1;
        }else if(pos == 8){
            x=2;y=2;
        }
        try{
            UniqueIdentifier gameId = proxy.startTrackedFlowDynamic(StartGameFlow.class,whoAmI,competeWith).getReturnValue().get();
            String submitTurn = proxy.startTrackedFlowDynamic(SubmitTurnFlow.class, gameId, whoAmI,competeWith,x,y).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED).body("Game Id Created: "+gameId+", "+whoAmI+" made the first move on position ["+x+","+y+"].");

        }catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PostMapping(value = "submitMove/{whoAmI}/{competeWith}/{position}")
    private ResponseEntity<String> submitMove(@PathVariable String whoAmI,
                                                         @PathVariable String competeWith,
                                                         @PathVariable String position) {
        int x  = -1;
        int y  = -1;
        int pos = Integer.parseInt(position);
        if(pos == 0) {
            x=0;y=0;
        }else if(pos == 1){
            x=0;y=1;
        }else if(pos == 2){
            x=0;y=2;
        }else if(pos == 3){
            x=1;y=0;
        }else if(pos == 4){
            x=1;y=1;
        }else if(pos == 5){
            x=1;y=2;
        }else if(pos == 6){
            x=2;y=0;
        }else if(pos == 7){
            x=2;y=1;
        }else if(pos == 8){
            x=2;y=2;
        }
        try{
            UniqueIdentifier gameId = proxy.startTrackedFlowDynamic(myGame.class,whoAmI).getReturnValue().get().getLinearId();
            String submitTurn = proxy.startTrackedFlowDynamic(SubmitTurnFlow.class, gameId, whoAmI,competeWith,x,y).getReturnValue().get();

            if(isGameOver(whoAmI)){
                proxy.startTrackedFlowDynamic(EndGameFlow.class, gameId, whoAmI,competeWith).getReturnValue().get();
                return ResponseEntity.status(HttpStatus.CREATED).body(""+whoAmI+" made the move on position ["+x+","+y+"], and Game Over");
            }else{
                return ResponseEntity.status(HttpStatus.CREATED).body(""+whoAmI+"+ made the move on position ["+x+","+y+"]");
            }
        }catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    private Boolean isGameOver(String whoAmI){
        //If the game is over, the Status should be a null variable
        //So if the status returned is GAME_IN_PROGRESS, it means the game is not over.
        try{
            BoardState.Status gameStatus = proxy.startTrackedFlowDynamic(myGame.class,whoAmI).getReturnValue().get().getStatus();
            return gameStatus != BoardState.Status.GAME_IN_PROGRESS;
        }catch (Exception e) {
            throw new IllegalArgumentException("No board");
        }
    }
}