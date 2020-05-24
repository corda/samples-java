package com.pr.consultant.initiator;

import co.paralleluniverse.fibers.Suspendable;
import com.pr.common.data.PRFlowData;
import com.pr.common.flow.PRFlow;
import com.pr.common.helper.PRFlowHelper;
import com.pr.contract.state.schema.contracts.PRContract;
import com.pr.contract.state.schema.states.PRState;
import com.pr.contract.state.schema.states.PRStatus;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.flows.CashPaymentFlow;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Currency;
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
public class ConsultantInitiator extends PRFlow {

    private final static Logger logger = LoggerFactory.getLogger(ConsultantInitiator.class);


    private PRState prState;
    private StateAndRef<PRState> previousPRState;
    private PRContract.Commands command;

    public ConsultantInitiator(PRState prState, StateAndRef<PRState> previousPRState,
                               PRContract.Commands command) throws FlowException {
        this.prState = prState;
        this.previousPRState = previousPRState;
        this.command = command;
    }

    public ConsultantInitiator (PRFlowData prFlowData) throws FlowException  {


        this(prFlowData.getNewPRState(),
                prFlowData.getPreviousPRState(),
                prFlowData.getCommand());

    }

    public ConsultantInitiator () throws FlowException {

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

        TransactionBuilder txBuilder = null;

        progressTracker.setCurrentStep(INITIALISING);
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        Command<PRContract.Commands> txCommand = new Command<>(command,
                prState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        progressTracker.setCurrentStep(BUILDING);
        validateTxCommand(command);


        if (command instanceof PRContract.Commands.CREATE){
            txBuilder = new TransactionBuilder(notary)
                    .withItems(new StateAndContract(prState, PRContract.PR_CONTRACT_ID),txCommand);
        }else {
            txBuilder = new TransactionBuilder(notary)
                    .withItems(new StateAndContract(prState,PRContract.PR_CONTRACT_ID),txCommand,previousPRState);
        }

        txBuilder.verify(getServiceHub());

        progressTracker.setCurrentStep(SIGNING);
        final SignedTransaction signTransaction = getServiceHub().signInitialTransaction(txBuilder);

        progressTracker.setCurrentStep(COLLECTING);
        Set<FlowSession> flowSessions = new HashSet<>();
        Party party = getOurIdentity();

        List<Party> parties = PRFlowHelper.getAllCounterParties(prState.getParticipants(),party, getServiceHub());

        for (Party counterParty: parties){
            flowSessions.add(initiateFlow(counterParty));
        }

        logger.info(" ********************************************** ");
        logger.info(" Parties :" + flowSessions.iterator().next().getCounterparty() );
        logger.info(" ********************************************** ");

        final SignedTransaction signedTransaction = subFlow(new CollectSignaturesFlow(signTransaction,flowSessions,COLLECTING.childProgressTracker()));

        progressTracker.setCurrentStep(FINALISING);
        SignedTransaction fullySignedTxFinal = subFlow(new FinalityFlow(signedTransaction,flowSessions, FINALISING.childProgressTracker()));

        return fullySignedTxFinal;
    }

    @NotNull
    private boolean validateTxCommand(PRContract.Commands command) {

        if (command instanceof PRContract.Commands.CREATE)
            return true;
        else
            throw new IllegalArgumentException("Unidentifiable command!");
    }
}
