package net.corda.examples.notarychange.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.notarychange.states.IOUState;

import java.util.Collections;

@InitiatingFlow
@StartableByRPC
public class SwitchNotaryFlow extends FlowLogic<String> {

    private UniqueIdentifier linearId;
    private Party newNotary;

    public SwitchNotaryFlow(UniqueIdentifier linearId, Party newNotary) {
        this.linearId = linearId;
        this.newNotary = newNotary;
    }

    private final ProgressTracker.Step QUERYING_VAULT = new ProgressTracker.Step("Fetching IOU from node's vault.");
    private final ProgressTracker.Step INITITATING_TRANSACTION = new ProgressTracker.Step("Initiating Notary Change Transaction"){
        @Override
        public ProgressTracker childProgressTracker() {
            return AbstractStateReplacementFlow.Instigator.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            QUERYING_VAULT,
            INITITATING_TRANSACTION
    );

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        progressTracker.setCurrentStep(QUERYING_VAULT);
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, Collections.singletonList(linearId.getId()));
        Vault.Page results  = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria);

        if(results.getStates().size() == 0){
            throw new FlowException("No IOU found for LinearId:" + linearId);
        }

        StateAndRef<IOUState> iouStateStateAndRef = (StateAndRef<IOUState>) results.getStates().get(0);
        progressTracker.setCurrentStep(INITITATING_TRANSACTION);
        subFlow(new NotaryChangeFlow<IOUState>(iouStateStateAndRef, newNotary, AbstractStateReplacementFlow.Instigator.Companion.tracker()));
        return "Notary Switched Successfully";
    }
}
