package net.corda.samples.client;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.samples.snl.flows.*;
import net.corda.samples.snl.oracle.flows.DiceRollerFlow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/snl/")
public class Controller {

    @Autowired
    private CordaRPCOps rpcProxy;

    @PostMapping("createGame")
    public APIResponse<String> createGame(@RequestBody Forms.CreateGameForm createGameForm) {
       try{
            String gameId = rpcProxy.startFlowDynamic(StartGameFlow.class, createGameForm.getPlayer1(),
                    createGameForm.getPlayer2()).getReturnValue().get();
            return APIResponse.success(gameId);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @PostMapping("playerMove")
    public APIResponse<Void> playMove(@RequestBody Forms.PlayerMoveForm playerMoveForm) {
        try{
            rpcProxy.startFlowDynamic(PlayerMoveFlow.Initiator.class, playerMoveForm.getPlayer(),
                        playerMoveForm.getGameId(),
                    Integer.valueOf(playerMoveForm.getRolledNumber())).getReturnValue().get();
            return APIResponse.success();
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("account/create/{name}")
    public APIResponse<Void> createAccount(@PathVariable String name){
        try{
            rpcProxy.startFlowDynamic(CreateAndShareAccountFlow.class, name).getReturnValue().get();
            return APIResponse.success();
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }


    @GetMapping("getGame/{gameId}")
    public APIResponse<GameInfo> getGame(@PathVariable String gameId){
        try{
            GameInfo gameInfo = rpcProxy.startFlowDynamic(QueyGameInfo.class, gameId).getReturnValue().get();
            return APIResponse.success(gameInfo);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

    @GetMapping("rollDice")
    public APIResponse<Integer> rollDice(){
        try{
            Integer numberRolled = rpcProxy.startFlowDynamic(QueryOracleForDiceRollFlow.class, "Test").getReturnValue().get();
            return APIResponse.success(numberRolled);
        }catch(Exception e){
            return APIResponse.error(e.getMessage());
        }
    }

}
