package net.corda.examples.spaceships.flows;

import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;

public interface SpaceshipFlows {

    class TokenizeSpaceship extends FlowLogic<SignedTransaction> {

        @Override
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }

    class BuyUniqueSpaceship extends FlowLogic<SignedTransaction> {

        @Override
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }

    class InvestInSpaceship extends FlowLogic<SignedTransaction> {

        @Override
        public SignedTransaction call() throws FlowException {
            return null;
        }
    }
}
