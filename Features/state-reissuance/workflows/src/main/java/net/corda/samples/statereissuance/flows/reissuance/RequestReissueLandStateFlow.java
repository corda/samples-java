package net.corda.samples.statereissuance.flows.reissuance;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.reissuance.flows.RequestReissuance;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.samples.statereissuance.contracts.LandTitleContract;
import net.corda.samples.statereissuance.states.LandTitleState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class RequestReissueLandStateFlow extends FlowLogic<SecureHash>{

    private Party issuer;
    private UniqueIdentifier plotIdentifier;


    public RequestReissueLandStateFlow(Party issuer, UniqueIdentifier plotIdentifier) {
        this.issuer = issuer;
        this.plotIdentifier = plotIdentifier;
    }

    @Override
    @Suspendable
    public SecureHash call() throws FlowException {

        List<StateAndRef<LandTitleState>> landTitleStateAndRefs = getServiceHub().getVaultService()
                .queryBy(LandTitleState.class).getStates();

        StateAndRef<LandTitleState> stateAndRef = landTitleStateAndRefs.stream().filter(landTitleStateAndRef -> {
            LandTitleState landTitleState = landTitleStateAndRef.getState().getData();
            return landTitleState.getLinearId().equals(plotIdentifier);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Land Not Found"));


        SecureHash txId = subFlow(new RequestReissuance<LandTitleState>(
                issuer,
                Arrays.asList(stateAndRef.getRef()),
                new LandTitleContract.Commands.Issue(),
                Collections.EMPTY_LIST,
                null)
        );

        return txId;
    }

}
