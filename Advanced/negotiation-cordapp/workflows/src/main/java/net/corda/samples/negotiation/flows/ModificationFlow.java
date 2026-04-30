package net.corda.samples.negotiation.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.crypto.keyrotation.crossprovider.KeyRotationProofChain;
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
import java.util.Map;

import static net.corda.core.internal.verification.AbstractVerifier.logger;

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

            // N.B.: Any party data retrieved from a state must be resolved using an instance of PartyIdentityResolver.
            //
            // `PartyIdentityResolver` checks whether the node has an associated proof for the given party.
            // If a proof is available, it resolves the party to the most recent valid identity permitted by that proof.
            //
            // Note that a key rotation proof is not immediately available after rotation. It typically becomes available
            // only after at least one transaction has been processed and the proof has been propagated to the Identity Service
            // of the node that performed the rotation.
            //
            // Therefore, immediately after a key rotation, the resolver may still return a valid but non-updated identity.
            // Once the proof becomes available, subsequent resolutions will return the updated (new-key) identity.
            //
            // The Identity Service will always contain the key rotation proofs for its own node.
            PartyIdentityResolver resolver = new PartyIdentityResolver(getServiceHub().getIdentityService());
            PartyIdentityResolved buyerKeyResolution = resolver.resolve(input.getBuyer());
            PartyIdentityResolved sellerKeyResolution = resolver.resolve(input.getSeller());
            PartyIdentityResolved proposerKeyResolution = resolver.resolve(input.getProposer());
            PartyIdentityResolved proposeeKeyResolution = resolver.resolve(input.getProposee());


            // A proof map must be included in the transaction command if any of the parties have rotated their keys,
            // and the input states contain the old identity while the output states contain the new identity.
            //
            // This is required because the contract must be able to verify the proof chains for all parties that have
            // undergone key rotation, and therefore must have access to the full proof history.
            //
            // All resolved parties are passed to the `generateProofChainMap` method, which determines whether there are
            // differences between the original and current identities. If differences are detected, it generates the
            // required proof chains and returns a map of new keys to their corresponding proof chains. If no differences
            // are found, an empty map is returned.
            //
            // In this example, only the buyer and seller are passed to `generateProofChainMap`, since the proposer and
            // proposee are always either the buyer or the seller.
            Map<PublicKey, KeyRotationProofChain> proofMap = PartyIdentityResolver.Companion.generateProofChainMap(buyerKeyResolution, sellerKeyResolution);
            if(proofMap.isEmpty()){
                logger.info("No proof.");
            } else {
                logger.info("One or more parties have rotated their keys, including the proof map in the transaction.");
            }

            // The `getOurIdentity` method always returns the most up-to-date identity for the node.
            //
            // In this context, it is safe to use the resolved party because if the node has performed a key rotation,
            // the resolver will return the latest valid identity. Therefore, it is safe to compare the identity returned
            // by `getOurIdentity` with the resolved identity.
            //
            // Never compare the identity returned by `getOurIdentity` with the original party stored in the state,
            // as that party may be outdated due to key rotation, and the resolver may return a different current identity.
            Party ourIdentityFromInput = (getOurIdentity().equals(proposerKeyResolution.getOriginalOrCurrentParty()))? proposerKeyResolution.getOriginalOrCurrentParty() : proposeeKeyResolution.getOriginalOrCurrentParty();
            Party counterpartyFromInput = (getOurIdentity().equals(proposerKeyResolution.getOriginalOrCurrentParty()))? proposeeKeyResolution.getOriginalOrCurrentParty() : proposerKeyResolution.getOriginalOrCurrentParty();

            // Creating the output using the newest identity provided by the resolver.
            //
            // This ensures that any outdated keys are replaced in the output state where possible.
            // If no updated identity is available, the existing (old) key is retained.
            //
            // Always use resolved parties when constructing the output state, as a key rotation proof
            // must be included in the transaction if any party identity has been updated.
            //
            // This guarantees that the transaction remains verifiable after key rotation.
            ProposalState output = new ProposalState(newAmount, buyerKeyResolution.getOriginalOrCurrentParty(), sellerKeyResolution.getOriginalOrCurrentParty(), ourIdentityFromInput, counterpartyFromInput, input.getLinearId());

            // Creating a command that includes the required signers and the proof map as part of the command data.
            //
            // Use resolved parties to determine the required signers, as the contract verifies signatures against resolved identities.
            // Using original state parties may lead to signature verification failures if those parties have undergone key rotation.
            // Consistency is required: the same resolved identities must be used both for determining required signers and for
            // constructing the output state. Old and new keys for a given node must not be mixed.
            //
            // The included proofs will be accessible to the contract during transaction verification via the command.
            List<PublicKey> requiredSigners = ImmutableList.of(proposeeKeyResolution.getOwningKey(), proposerKeyResolution.getOwningKey());
            Command command = new Command(new ProposalAndTradeContract.Commands.Modify(), requiredSigners, proofMap);

            // Building the transaction
            Party notary = inputStateAndRef.getState().getNotary();
            TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(inputStateAndRef)
                    .addOutputState(output, ProposalAndTradeContract.ID)
                    .addCommand(command);

            // Signing the transaction ourselves
            SignedTransaction partStx = getServiceHub().signInitialTransaction(txBuilder);

            // Gathering the counterparty's signatures
            // Note that the counterparty may have also rotated their keys. The initiateFlow will automatically resolve the counterparty's
            // identity and select the correct key to use for the session, so we can simply pass in the counterparty as retrieved from the resolver.
            FlowSession counterpartySession = initiateFlow(counterpartyFromInput);
            SignedTransaction fullyStx = subFlow(new CollectSignaturesFlow(partStx, ImmutableList.of(counterpartySession)));

            // Finalising the transaction
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

                        // The counterparty session always provides the most up-to-date identity for the counterparty.
                        //
                        // Therefore, any party retrieved from a state must be resolved using `resolveToCurrentParty`.
                        // While `resolveToCurrentParty` does not rely on a proof, it resolves the party to its latest valid identity.
                        //
                        // This ensures that equality checks behave as expected after key rotation.
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
