package net.corda.samples.snl.oracle.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.samples.snl.service.DiceRollService;

@InitiatedBy(DiceRollerFlow.class)
public class DiceRollerFlowHandler extends FlowLogic<Void> {

    private FlowSession requestSession;

    public DiceRollerFlowHandler(FlowSession requestSession) {
        this.requestSession = requestSession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        String player = requestSession.receive(String.class).unwrap(it -> it);
        DiceRollService service = getServiceHub().cordaService(DiceRollService.class);
        int roll = service.diceRoll(player);
        requestSession.send(roll);
        return null;
    }
}
