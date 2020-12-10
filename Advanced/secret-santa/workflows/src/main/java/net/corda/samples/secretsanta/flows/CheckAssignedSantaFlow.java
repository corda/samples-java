package net.corda.samples.secretsanta.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.samples.secretsanta.states.SantaSessionState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * This flow will create the account on the node on which you run this flow. This is done using inbuilt flow called CreateAccount.
 * CreateAccount creates an AccountInfo object which has name, host and id as its fields. This is mapped to Account table in the db.
 * For any other party to transact with this account, this AccountInfo will have to be shared with that Party.
 * Hence the Ipl Ticket Dealers create the ticket buyers accounts on their end and share this accountInfo with the Bank node and BCCI node.
 */
@StartableByRPC
@InitiatingFlow
public class CheckAssignedSantaFlow extends FlowLogic<SantaSessionState> {

    private UniqueIdentifier santaSessionId;
    private SantaSessionState santaSessionState = null;

    public CheckAssignedSantaFlow(UniqueIdentifier santaSessionId) {
        this.santaSessionId = santaSessionId;
    }

    @Override
    @Suspendable
    public SantaSessionState call() throws FlowException {

        // Retrieve the Game State from the vault using LinearStateQueryCriteria
        List<UUID> listOfLinearIds = Arrays.asList(santaSessionId.getId());
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
        Vault.Page results = getServiceHub().getVaultService().queryBy(SantaSessionState.class, queryCriteria);

        if (results.getStates().size() < 1) {
            throw new IllegalArgumentException("No corresponding GameID Found.");
        }

        santaSessionState = (SantaSessionState) ((StateAndRef) results.getStates().get(0)).getState().getData();

        return santaSessionState;
    }

    public UniqueIdentifier getSantaSessionId() {
        return santaSessionId;
    }

    public SantaSessionState getSantaSessionState() {
        return santaSessionState;
    }
}

