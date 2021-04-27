package net.corda.samples.contractsdk.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.samples.contractsdk.contracts.RecordPlayerContract;
import net.corda.samples.contractsdk.states.Needle;
import net.corda.samples.contractsdk.states.RecordPlayerState;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class UpdateRecordPlayerFlow extends FlowLogic<SignedTransaction> {

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    String needleId;

    Needle needle;
    int magneticStrength;
    int coilTurns;
    int amplifierSNR;
    int songsPlayed;
    UniqueIdentifier stateId;

    /*
     * A new record player is issued only from the manufacturer to an exclusive dealer.
     * Most of the settings are default
     */
    public UpdateRecordPlayerFlow(UniqueIdentifier stateId, String needleId, int magneticStrength, int coilTurns, int amplifierSNR, int songsPlayed) {

        if (needleId.toLowerCase().equals("elliptical")) {
            needle = Needle.ELLIPTICAL;
        }
        if (needleId.toLowerCase().equals("damaged")) {
            needle = Needle.DAMAGED;
        } else {
            throw new IllegalArgumentException("Invalid needle state given.");
        }

        this.stateId = stateId;
        this.magneticStrength = magneticStrength;
        this.coilTurns = coilTurns;
        this.amplifierSNR = amplifierSNR;
        this.songsPlayed = songsPlayed;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        List<UUID> listOfLinearIds = Arrays.asList(stateId.getId());
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, listOfLinearIds);
        Vault.Page results = getServiceHub().getVaultService().queryBy(RecordPlayerState.class, queryCriteria);
        StateAndRef inputStateAndRef = (StateAndRef) results.getStates().get(0);
        RecordPlayerState input = (RecordPlayerState) ((StateAndRef) results.getStates().get(0)).getState().getData();

        Party manufacturer = input.getManufacturer();
        Party dealer = input.getDealer();

        if (getOurIdentity() == input.getDealer()) {
            throw new IllegalArgumentException("Only the dealer that sold this record player can service it!");
        }

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        Command<RecordPlayerContract.Commands.Update> command = new Command<>(
                new RecordPlayerContract.Commands.Update(),
                Arrays.asList(manufacturer.getOwningKey(), dealer.getOwningKey())
        );

        // Create a new TransactionBuilder object.
        final TransactionBuilder builder = new TransactionBuilder(notary);

        // add an output state
        builder.addInputState(inputStateAndRef);
        builder.addOutputState(input.update(needle, magneticStrength, coilTurns, amplifierSNR, songsPlayed), RecordPlayerContract.ID);
        builder.addCommand(command);

        // Verify and sign it with our KeyPair.
        builder.verify(getServiceHub());
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder);

        // Collect the other party's signature using the SignTransactionFlow.
        List<Party> otherParties = input.getParticipants().stream().map(el -> (Party) el).collect(Collectors.toList());
        otherParties.remove(getOurIdentity());

        List<FlowSession> sessions = otherParties.stream().map(el -> initiateFlow(el)).collect(Collectors.toList());

        SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions));

        // Assuming no exceptions, we can now finalise the transaction
        return subFlow(new FinalityFlow(stx, sessions));
    }
}



