package net.corda.samples.negotiation.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;

import net.corda.core.crypto.keyrotation.crossprovider.KeyRotationProofChain;
import net.corda.core.crypto.keyrotation.crossprovider.PartyIdentityResolved;
import net.corda.core.crypto.keyrotation.crossprovider.PartyIdentityResolver;
import net.corda.samples.negotiation.contracts.ProposalAndTradeContract;
import net.corda.samples.negotiation.states.ProposalState;
import net.corda.samples.negotiation.states.TradeState;
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
import java.util.Map;

import static net.corda.core.internal.verification.AbstractVerifier.logger;

public class AcceptanceFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private UniqueIdentifier proposalId;
        private ProgressTracker progressTracker = new ProgressTracker();

        public Initiator(UniqueIdentifier proposalId) {
            this.proposalId = proposalId;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            QueryCriteria.LinearStateQueryCriteria inputCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(proposalId), Vault.StateStatus.UNCONSUMED,null);

            StateAndRef inputStateAndRef = getServiceHub().getVaultService().queryBy(ProposalState.class, inputCriteria).getStates().get(0);

            ProposalState input = (ProposalState) inputStateAndRef.getState().getData();

            // The parties are being resolved so that we can move way from using possible outdated keys from the input state
            // and instead use the most up-to-date keys when building the transaction.
            PartyIdentityResolver resolver = new PartyIdentityResolver(getServiceHub().getIdentityService());
            PartyIdentityResolved buyerKeyResolution = resolver.resolve(input.getBuyer());
            PartyIdentityResolved sellerKeyResolution = resolver.resolve(input.getSeller());
            PartyIdentityResolved proposerKeyResolution = resolver.resolve(input.getProposer());
            PartyIdentityResolved proposeeKeyResolution = resolver.resolve(input.getProposee());

            Map<PublicKey, KeyRotationProofChain> proofMap = PartyIdentityResolver.Companion.generateProofChainMap(buyerKeyResolution, sellerKeyResolution);
            if(proofMap.isEmpty()){
                logger.info("No proof.");
            } else {
                logger.info("One or more parties have rotated their keys, including the proof map in the transaction.");
            }

            // Creating the output
            TradeState output = new TradeState(input.getAmount(), buyerKeyResolution.getOriginalOrCurrentParty(), sellerKeyResolution.getOriginalOrCurrentParty(), input.getLinearId());

            // Creating the command
            List<PublicKey> requiredSigners = ImmutableList.of(proposeeKeyResolution.getOwningKey(), proposerKeyResolution.getOwningKey());
            Command command = new Command(new ProposalAndTradeContract.Commands.Accept(), requiredSigners, proofMap);

            // Building the transaction
            Party notary = inputStateAndRef.getState().getNotary();
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(output, ProposalAndTradeContract.ID)
                    .addCommand(command);

            // Signing the transaction ourselves
            SignedTransaction partStx = getServiceHub().signInitialTransaction(txBuilder);

            // Gathering the counterparty's signature.
            //
            // The identity returned by `getOurIdentity` cannot be compared directly with the proposer from the input state,
            // as the node may have rotated its keys since the proposal was created. The resolved party must be used instead.
            // The resolver will always be able to resolve the node's own identity because the proof will always be available for the node's own key.
            Party counterparty = (getOurIdentity().equals(proposerKeyResolution.getOriginalOrCurrentParty())) ? input.getProposee() : input.getProposer();

            // The counterparty might be an old key, but the session will be initiated with the most up-to-date identity.
            // No need to use the resolved party in this case.
            FlowSession counterpartySession = initiateFlow(counterparty);
            SignedTransaction fullyStx = subFlow(new CollectSignaturesFlow(partStx, ImmutableList.of(counterpartySession)));

            // Finalising the transaction
            SignedTransaction finalisedTx  = subFlow(new FinalityFlow(fullyStx, ImmutableList.of(counterpartySession)));
            return finalisedTx;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction>{
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

                        // The counterparty session always provides the most up-to-date identity for the counterparty.
                        //
                        // Therefore, any party retrieved from a state must be resolved using `resolveToCurrentParty`.
                        // While `resolveToCurrentParty` does not rely on a proof, it resolves the party to its latest valid identity.
                        //
                        // This ensures that equality checks behave as expected after key rotation.
                        Party proposee = PartyIdentityResolver.Companion.resolveToCurrentParty(input.getProposee(), getServiceHub().getIdentityService());
                        if(!proposee.equals(counterpartySession.getCounterparty())){
                            throw new FlowException("Only the proposee can accept a proposal.");
                        }
                    } catch (SignatureException e) {
                        throw new FlowException("Check transaction failed");
                    }


                }
            };
            SecureHash txId = subFlow(signTransactionFlow).getId();
            SignedTransaction finalisedTx = subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
            return finalisedTx;
        }
    }
}


