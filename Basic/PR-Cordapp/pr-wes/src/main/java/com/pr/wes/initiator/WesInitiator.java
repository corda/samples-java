package com.pr.wes.initiator;

import co.paralleluniverse.fibers.Suspendable;
import com.pr.common.data.PRFlowData;
import com.pr.common.flow.PRFlow;
import com.pr.common.helper.PRFlowHelper;
import com.pr.contract.state.schema.contracts.PRContract;
import com.pr.contract.state.schema.states.PRState;
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

// ******************
// * Initiator flow *
// ******************
@StartableByService
@StartableByRPC
public class WesInitiator extends PRFlow {


    private final static Logger logger = LoggerFactory.getLogger(WesInitiator.class);


    private PRState newPRState;
    private StateAndRef<PRState> previousPRState;
    private PRContract.Commands command;

    public WesInitiator(PRState prState, StateAndRef<PRState> previousPRState,
                               PRContract.Commands command) throws FlowException {
        this.newPRState = prState;
        this.previousPRState = previousPRState;
        this.command = command;
    }

    public WesInitiator (PRFlowData prFlowData) throws FlowException  {


        this(prFlowData.getNewPRState(),
                prFlowData.getPreviousPRState(),
                prFlowData.getCommand());

    }

    public WesInitiator () throws FlowException {

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
        // Initiator flow logic goes here.
        final Party notary;
        Command<PRContract.Commands> txCommand;
        TransactionBuilder txBuilder = null;
        final SignedTransaction signTransaction;
        Set<FlowSession> flowSessions = null;
        Party party;
        List<Party> allCounterParties = null;
        final SignedTransaction signedTransaction;
        SignedTransaction fullySignedTxFinal;



        progressTracker.setCurrentStep(INITIALISING);
        notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        validateTxCommand(command);

        txCommand = new Command<>(command,
                newPRState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        progressTracker.setCurrentStep(BUILDING);
        txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(newPRState, PRContract.PR_CONTRACT_ID), txCommand, previousPRState);

        txBuilder.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING);
        signTransaction = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(COLLECTING);
        flowSessions = new HashSet<>();
        party = getOurIdentity();

        allCounterParties = PRFlowHelper.getAllCounterParties(newPRState.getParticipants(),party, getServiceHub());

        for (Party counterParty: allCounterParties){
            flowSessions.add(initiateFlow(counterParty));
        }

        logger.info(" ********************************************** ");
        logger.info(" Parties :" + flowSessions.iterator().next().getCounterparty() );
        logger.info(" ********************************************** ");

        signedTransaction = subFlow(new CollectSignaturesFlow(signTransaction,flowSessions,COLLECTING.childProgressTracker()));

        progressTracker.setCurrentStep(FINALISING);
        fullySignedTxFinal = subFlow(new FinalityFlow(signedTransaction,flowSessions, FINALISING.childProgressTracker()));

        return fullySignedTxFinal;
    }

    @NotNull
    private boolean validateTxCommand(PRContract.Commands command) {

        if (command instanceof PRContract.Commands.RequestApproval)
            return true;
        else
            throw new IllegalArgumentException("Unidentifiable command!");
    }
}
