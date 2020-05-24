package com.pr.common.data;

import com.pr.student.contract.state.schema.contract.RequestFormContract;
import com.pr.student.contract.state.schema.state.RequestForm;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.serialization.CordaSerializable;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

@CordaSerializable
public class RequestFlowData {
    private RequestForm newRequestState;
    private StateAndRef<RequestForm> previousRequestState;
    private RequestFormContract.Commands command;


    public RequestFlowData(RequestForm newRequestState,
                           StateAndRef<RequestForm> previousRequestState,
                           RequestFormContract.Commands command) {
        this.newRequestState = newRequestState;
        this.previousRequestState = previousRequestState;
        this.command = command;
    }

    public RequestForm getNewRequestState() {
        return newRequestState;
    }

    public StateAndRef<RequestForm> getPreviousRequestState() {
        return previousRequestState;
    }

    public RequestFormContract.Commands getCommand() {
        return command;
    }
}
