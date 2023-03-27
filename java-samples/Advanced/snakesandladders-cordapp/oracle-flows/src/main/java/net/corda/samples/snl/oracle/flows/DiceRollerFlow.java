package net.corda.samples.snl.oracle.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;

@InitiatingFlow
public class DiceRollerFlow extends FlowLogic<Integer> {

    private String player;
    private Party oracle;

    public DiceRollerFlow(String player, Party oracle) {
        this.player = player;
        this.oracle = oracle;
    }

    @Override
    @Suspendable
    public Integer call() throws FlowException {
        return initiateFlow(oracle).sendAndReceive(Integer.class, player).unwrap(it->it);
    }
}
