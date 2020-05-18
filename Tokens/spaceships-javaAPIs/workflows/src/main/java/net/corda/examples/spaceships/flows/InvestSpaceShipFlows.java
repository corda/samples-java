package net.corda.examples.spaceships.flows;

import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;

public interface InvestSpaceShipFlows {
    class InvestInSpaceship extends FlowLogic<SignedTransaction> {

        @Override
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }
}
