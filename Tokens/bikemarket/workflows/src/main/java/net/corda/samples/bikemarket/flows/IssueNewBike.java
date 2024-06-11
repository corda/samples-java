package net.corda.samples.bikemarket.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.NonFungibleTokenBuilder;
import net.corda.samples.bikemarket.states.FrameTokenState;
import net.corda.samples.bikemarket.states.WheelsTokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

@InitiatingFlow
@StartableByRPC
public class IssueNewBike extends FlowLogic<String> {

    private final String frameSerial;
    private final String wheelsSerial;
    private final Party holder;

    public IssueNewBike(String frameSerial, String wheelsSerial, Party holder) {
        this.frameSerial = frameSerial;
        this.wheelsSerial = wheelsSerial;
        this.holder = holder;
    }

    @Suspendable
    @Override
    public String call() throws FlowException {

        //Step 1: Frame Token
        //get frame states on ledger
        StateAndRef<FrameTokenState> frameStateAndRef = getServiceHub().getVaultService().
                queryBy(FrameTokenState.class).getStates().stream()
                .filter(sf->sf.getState().getData().getserialNum().equals(this.frameSerial)).findAny()
                .orElseThrow(()-> new IllegalArgumentException("wheel token with serial=" + this.frameSerial + " not found from vault"));

        //get the TokenType object
        FrameTokenState frametokentype = frameStateAndRef.getState().getData();

        //mention the current holder also
        NonFungibleToken frametoken = new NonFungibleTokenBuilder()
                .ofTokenType(frametokentype.toPointer(frametokentype.getClass()))
                .issuedBy(getOurIdentity())
                .heldBy(holder)
                .buildNonFungibleToken();

        //Step 2: Wheels Token
        StateAndRef<WheelsTokenState> wheelStateStateAndRef = getServiceHub().getVaultService().
                queryBy(WheelsTokenState.class).getStates().stream().filter(sf->sf.getState().getData().getserialNum().equals(this.wheelsSerial)).findAny()
                .orElseThrow(()-> new IllegalArgumentException("wheel token with serial=" + this.wheelsSerial + " not found from vault"));

        //get the TokenType object
        WheelsTokenState wheeltokentype = wheelStateStateAndRef.getState().getData();

        //mention the current holder also
        NonFungibleToken wheeltoken = new NonFungibleTokenBuilder()
                .ofTokenType(wheeltokentype.toPointer(wheeltokentype.getClass()))
                .issuedBy(getOurIdentity())
                .heldBy(holder)
                .buildNonFungibleToken();

        //distribute the new bike (two token to be exact)
        //call built in flow to issue non fungible tokens
        SignedTransaction stx = subFlow(new IssueTokens(ImmutableList.of(frametoken,wheeltoken)));

        return "\nA new bike is being issued to "+ this.holder.getName().getOrganisation() + " with frame serial: "
                + this.frameSerial + "; wheels serial: "+ this.wheelsSerial + "\nTransaction ID: " + stx.getId();
    }
}
