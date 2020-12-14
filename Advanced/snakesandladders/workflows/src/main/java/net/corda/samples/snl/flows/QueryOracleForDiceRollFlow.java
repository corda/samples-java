package net.corda.samples.snl.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.samples.snl.oracle.flows.DiceRollerFlow;

@StartableByRPC
public class QueryOracleForDiceRollFlow extends FlowLogic<Integer> {

    private String player;

    public QueryOracleForDiceRollFlow(String player) {
        this.player = player;
    }

    @Override
    @Suspendable
    public Integer call() throws FlowException {
        Party oracle = getServiceHub().getNetworkMapCache()
                .getNodeByLegalName(CordaX500Name.parse("O=Oracle,L=Mumbai,C=IN")).getLegalIdentities().get(0);

        return subFlow(new DiceRollerFlow(player, oracle));
    }
}
