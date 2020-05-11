package com.t20worldcup.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.t20worldcup.states.T20CricketTicket;
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;

/**
 * This flow should be run by BCCI node. BCCI node will take care of issuing the base token type for the Ipl ticket. The token type will be craeted on the BCCI node
 * and need not be shared with anyone. Later BCCI will Issue NonFungible tokens on this base type.
 * Ipl base type will be of type EvolvableToken. Keep in mind that the maintainer of the EvolvableTokenType is of type Party and not of type AnonymousParty
 */
@StartableByRPC
@InitiatingFlow
public class CreateT20CricketTicketTokenFlow extends FlowLogic {

    private final String ticketTeam;

    public CreateT20CricketTicketTokenFlow(String ticketTeam) {
        this.ticketTeam = ticketTeam;
    }

    @Override
    @Suspendable
    public Object call() throws FlowException {
        //get the notary
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //create token type by passing in the name of the ipl match. specify the maintainer as BCCI
        T20CricketTicket t20CricketTicket = new T20CricketTicket(new UniqueIdentifier(), ticketTeam, getOurIdentity());

        //warp it with transaction state specifying the notary
        TransactionState transactionState = new TransactionState(t20CricketTicket, notary);

        //call built in sub flow CreateEvolvableTokens to craete the base type on BCCI node
        return subFlow(new CreateEvolvableTokens(transactionState));
    }
}
