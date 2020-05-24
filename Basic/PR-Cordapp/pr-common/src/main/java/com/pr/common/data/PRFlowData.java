package com.pr.common.data;

import com.pr.contract.state.schema.contracts.PRContract;
import com.pr.contract.state.schema.states.PRState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.serialization.CordaSerializable;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@CordaSerializable
public class PRFlowData {
    private PRState newPRState;
    private StateAndRef<PRState> previousPRState;
    private PRContract.Commands command;

    public PRFlowData(PRState newPRState, StateAndRef<PRState> previousPRState, PRContract.Commands command) {
        this.newPRState = newPRState;
        this.previousPRState = previousPRState;
        this.command = command;
    }


    public PRState getNewPRState() {
        return newPRState;
    }

    public StateAndRef<PRState> getPreviousPRState() {
        return previousPRState;
    }

    public PRContract.Commands getCommand() {
        return command;
    }


}

