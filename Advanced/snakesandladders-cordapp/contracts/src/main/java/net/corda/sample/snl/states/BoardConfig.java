package net.corda.sample.snl.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.sample.snl.contracts.BoardConfigContract;
import net.corda.sample.snl.contracts.GameBoardContract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@BelongsToContract(BoardConfigContract.class)
public class BoardConfig implements ContractState {

    private Map<Integer, Integer> ladderPositions;
    private Map<Integer, Integer> snakePositions;
    private List<AbstractParty> players;

    public BoardConfig(Map<Integer, Integer> ladderPositions, Map<Integer, Integer> snakePositions, List<AbstractParty> players) {
        this.ladderPositions = ladderPositions;
        this.snakePositions = snakePositions;
        this.players = players;
    }

    public Map<Integer, Integer> getLadderPositions() {
        return ladderPositions;
    }

    public Map<Integer, Integer> getSnakePositions() {
        return snakePositions;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return players;
    }
}
