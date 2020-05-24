package com.pr.consultant.initiator;

import co.paralleluniverse.fibers.Suspendable;
import com.pr.common.data.RequestFlowData;
import com.pr.common.exception.PRException;
import com.pr.common.flow.RequestFormFlow;
import com.pr.common.helper.PRFlowHelper;
import com.pr.student.contract.state.schema.contract.RequestFormContract;
import com.pr.student.contract.state.schema.state.RequestForm;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@StartableByService
@StartableByRPC
public class RequestFlowInitiator extends RequestFormFlow {
    private static final Logger logger = LoggerFactory.getLogger(RequestFlowInitiator.class);
    private RequestForm newRequestState;
    private StateAndRef<RequestForm> previousRequestState;
    private RequestFormContract.Commands command;

    public RequestFlowInitiator(RequestForm newRequestState,
                                StateAndRef<RequestForm> previousRequestState,
                                RequestFormContract.Commands command) {
        this.newRequestState = newRequestState;
        this.previousRequestState = previousRequestState;
        this.command = command;
    }

    public RequestFlowInitiator(RequestFlowData data) {
        this(data.getNewRequestState(),
                data.getPreviousRequestState(),
                data.getCommand());
    }

    public RequestFlowInitiator() {

    }

    private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
    private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Building the Transaction.");
    private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
    private final ProgressTracker.Step COLLECTING = new ProgressTracker.Step("Collecting counterparty signature.") {

        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };

    private final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {

        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING
    );


    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }


    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        // Generate an unsigned transaction.
        TransactionBuilder txBuilder = null;
        // Create appropriate Command.

        validateTxCommand(command);

        Command<RequestFormContract.Commands> txCommand = new Command<>(command,
                newRequestState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        progressTracker.setCurrentStep(BUILDING);
        if (command instanceof RequestFormContract.Commands.CREATE) {
            txBuilder = new TransactionBuilder(notary)
                    .withItems(new StateAndContract(newRequestState, RequestFormContract.REQUEST_CONTRACT_ID), txCommand);
        } else {
            txBuilder = new TransactionBuilder(notary)
                    .withItems(new StateAndContract(newRequestState, RequestFormContract.REQUEST_CONTRACT_ID), txCommand, previousRequestState);
        }

        try {
            // Verify the transaction.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            // Sign the transaction.
            progressTracker.setCurrentStep(SIGNING);
            final SignedTransaction signTransaction = getServiceHub().signInitialTransaction(txBuilder);

            progressTracker.setCurrentStep(COLLECTING);
            Set<FlowSession> flowSessions = new HashSet<>();
            Party party = getOurIdentity();

            List<Party> parties = PRFlowHelper.getAllCounterParties(newRequestState.getParticipants(), party, getServiceHub());

            for (Party counterParty : parties) {
                flowSessions.add(initiateFlow(counterParty));
            }

            logger.info(" ********************************************** ");
            logger.info(" Parties :" + flowSessions.iterator().next().getCounterparty());
            logger.info(" ********************************************** ");

            final SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(signTransaction, flowSessions, COLLECTING.childProgressTracker()));

            progressTracker.setCurrentStep(FINALISING);
            SignedTransaction fullySignedTxFinal = subFlow(new FinalityFlow(signedTransaction, flowSessions, FINALISING.childProgressTracker()));
            return fullySignedTxFinal;
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            throw new FlowException(e.getMessage());
        }
    }

    @NotNull
    private void validateTxCommand(RequestFormContract.Commands command) {

        if (command instanceof RequestFormContract.Commands.CREATE
                || command instanceof RequestFormContract.Commands.UPDATE) {
            return;
        } else {
            throw new PRException("Unidentifiable command! : " + command.toString());
        }
    }
}
