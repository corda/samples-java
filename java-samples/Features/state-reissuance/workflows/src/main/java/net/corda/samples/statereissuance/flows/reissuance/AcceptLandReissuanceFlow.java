package net.corda.samples.statereissuance.flows.reissuance;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.reissuance.flows.ReissueStates;
import com.r3.corda.lib.reissuance.states.ReissuanceRequest;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.samples.statereissuance.flows.ExitLandTitleFlow;

import java.util.Arrays;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class AcceptLandReissuanceFlow extends FlowLogic<SecureHash> {

    private StateRef stateRef;
    private Party issuer;

    public AcceptLandReissuanceFlow(StateRef stateRef, Party issuer) {
        this.stateRef = stateRef;
        this.issuer = issuer;
    }

    @Override
    @Suspendable
    public SecureHash call() throws FlowException {

        List<StateAndRef<ReissuanceRequest>> reissueRequestStateAndRefs = getServiceHub().getVaultService()
                .queryBy(ReissuanceRequest.class).getStates();

        StateAndRef<ReissuanceRequest> stateAndRef = reissueRequestStateAndRefs.stream().filter(reissuanceRequestStateAndRef -> {
            ReissuanceRequest reissuanceRequest = reissuanceRequestStateAndRef.getState().getData();
            return reissuanceRequest.getStateRefsToReissue().contains(stateRef);
        }).findAny().orElseThrow(() -> new IllegalArgumentException("ReIssuance Request does not exist"));

        SecureHash txHash = subFlow(new ReissueStates<>(stateAndRef, Arrays.asList(issuer)));

        return txHash;
    }
}
