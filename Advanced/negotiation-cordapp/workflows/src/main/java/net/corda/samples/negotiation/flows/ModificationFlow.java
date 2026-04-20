package net.corda.samples.negotiation.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.crypto.keyrotation.crossprovider.PartyIdentityResolved;
import net.corda.core.crypto.keyrotation.crossprovider.PartyIdentityResolver;
import net.corda.samples.negotiation.contracts.ProposalAndTradeContract;
import net.corda.samples.negotiation.states.ProposalState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.List;

public class ModificationFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private UniqueIdentifier proposalId;
        private int newAmount;
        private ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(UniqueIdentifier proposalId, int newAmount) {
            this.proposalId = proposalId;
            this.newAmount = newAmount;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(proposalId), Vault.StateStatus.UNCONSUMED, null);
            StateAndRef inputStateAndRef = getServiceHub().getVaultService().queryBy(ProposalState.class, inputCriteria).getStates().get(0);
            ProposalState input = (ProposalState) inputStateAndRef.getState().getData();
            Party proposerParty = PartyIdentityResolver.Companion.resolveToCurrentParty(input.getProposer(), getServiceHub().getIdentityService());

            //Creating the output
            Party myPartyFromInput = (getOurIdentity().equals(proposerParty)) ? input.getProposer() : input.getProposee();
            Party counterpartyFromInput = (myPartyFromInput.equals(input.getProposer()))? input.getProposee() : input.getProposer();
            ProposalState output = new ProposalState(newAmount, input.getBuyer(),input.getSeller(), myPartyFromInput, counterpartyFromInput, input.getLinearId());

            //Creating the command
            List<PublicKey> requiredSigners = ImmutableList.of(input.getProposee().getOwningKey(), input.getProposer().getOwningKey());
            Command command = new Command(new ProposalAndTradeContract.Commands.Modify(), requiredSigners);

            //Building the transaction
            Party notary = inputStateAndRef.getState().getNotary();
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(output, ProposalAndTradeContract.ID)
                    .addCommand(command);

            //Signing the transaction ourselves
            SignedTransaction partStx = getServiceHub().signInitialTransaction(txBuilder);

            //Gathering the counterparty's signatures
            FlowSession counterpartySession = initiateFlow(counterpartyFromInput);
            SignedTransaction fullyStx = subFlow(new CollectSignaturesFlow(partStx, ImmutableList.of(counterpartySession)));

            //Finalising the transaction
            SignedTransaction finalTx = subFlow(new FinalityFlow(fullyStx,ImmutableList.of(counterpartySession)));
            return finalTx;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private FlowSession counterpartySession;

        public Responder(FlowSession counterpartySession) {
            this.counterpartySession = counterpartySession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            SignTransactionFlow signTransactionFlow = new SignTransactionFlow(counterpartySession){

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                    try {
                        LedgerTransaction ledgerTx = stx.toLedgerTransaction(getServiceHub(), false);
                        ProposalState input = ledgerTx.inputsOfType(ProposalState.class).get(0);
                        Party proposee = PartyIdentityResolver.Companion.resolveToCurrentParty(input.getProposee(), getServiceHub().getIdentityService());
                        if(!proposee.equals(counterpartySession.getCounterparty())){
                            throw new FlowException("Only the proposee can modify a proposal.");
                        }
                    } catch (SignatureException e) {
                        throw new FlowException();
                    }
                }
            };
            SecureHash txId = subFlow(signTransactionFlow).getId();

            SignedTransaction finalisedTx = subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
            return finalisedTx;
        }
    }
}
