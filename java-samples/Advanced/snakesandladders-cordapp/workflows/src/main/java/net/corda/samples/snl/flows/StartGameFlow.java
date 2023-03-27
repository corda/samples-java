package net.corda.samples.snl.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.transactions.SignedTransaction;

@StartableByRPC
public class StartGameFlow extends FlowLogic<String> {

    private String player1;
    private String player2;

    public StartGameFlow(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        subFlow(new CreateBoardConfig.Initiator(player1, player2));
        return subFlow(new CreateGameFlow.Initiator(player1, player2));
    }
}
