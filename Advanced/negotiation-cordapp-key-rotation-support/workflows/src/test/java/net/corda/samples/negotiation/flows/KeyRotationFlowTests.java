package net.corda.samples.negotiation.flows;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.crypto.keyrotation.crossprovider.KeyRotationProofChain;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.negotiation.states.ProposalState;
import net.corda.samples.negotiation.states.TradeState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KeyRotationFlowTests extends KeyRotationTestBase {

    @Test
    public void nodeARotatesAndNodeAStartsTheTransaction() throws Exception {
        Party partyAOriginal = identityOf(a);
        Party partyB = identityOf(b);

        // Node B proposes to Node A.
        UniqueIdentifier proposalId = propose(b, 1, partyAOriginal);
        assertProposal(proposalOf(a, proposalId), partyB, partyAOriginal, partyB, partyAOriginal, 1);
        assertProposal(proposalOf(b, proposalId), partyB, partyAOriginal, partyB, partyAOriginal, 1);

        a = rotate(a);
        Party partyARotated = identityOf(a);

        // 1. Node A modifies.
        SignedTransaction modifiedByA = modify(a, proposalId, 2);
        assertProposal(outputOf(modifiedByA), partyB, partyAOriginal, partyAOriginal, partyB, 2);
        assertRotationProof(modifiedByA, partyAOriginal, partyARotated);

        // 2. Node B modifies.
        SignedTransaction modifiedByB = modify(b, proposalId, 3);
        assertProposal(outputOf(modifiedByB), partyB, partyAOriginal, partyB, partyAOriginal, 3);
        assertRotationProof(modifiedByB, partyAOriginal, partyARotated);

        // 3. Node A modifies again.
        assertRotationProof(modify(a, proposalId, 4), partyAOriginal, partyARotated);

        // 4. Node B accepts
        SignedTransaction accepted = accept(b, proposalId);
        assertRotationProof(accepted, partyAOriginal, partyARotated);

        TradeState trade = (TradeState) accepted.getCoreTransaction().getOutputStates().get(0);
        assertEquals(partyAOriginal, trade.getSeller());
        assertEquals(partyB, trade.getBuyer());
    }

    @Test
    public void nodeARotatesAndNodeBStartsTheTransaction() throws Exception {
        Party partyAOriginal = identityOf(a);
        Party partyB = identityOf(b);

        // Node A proposes to Node B.
        UniqueIdentifier proposalId = propose(a, 1, partyB);
        assertProposal(proposalOf(a, proposalId), partyAOriginal, partyB, partyAOriginal, partyB, 1);
        assertProposal(proposalOf(b, proposalId), partyAOriginal, partyB, partyAOriginal, partyB, 1);

        a = rotate(a);
        Party partyARotated = identityOf(a);

        // 1. Node B modifies.
        SignedTransaction modifiedByB = modify(b, proposalId, 2);
        assertProposal(outputOf(modifiedByB), partyAOriginal, partyB, partyB, partyAOriginal, 2);
        assertRotationProof(modifiedByB, partyAOriginal, partyARotated);

        // 2. Node A modifies.
        SignedTransaction modifiedByA = modify(a, proposalId, 3);
        assertProposal(outputOf(modifiedByA), partyAOriginal, partyB, partyAOriginal, partyB, 3);
        assertRotationProof(modifiedByA, partyAOriginal, partyARotated);

        // 3. Node B modifies again.
        assertRotationProof(modify(b, proposalId, 4), partyAOriginal, partyARotated);

        // 4. Node A accepts
        SignedTransaction accepted = accept(a, proposalId);
        assertRotationProof(accepted, partyAOriginal, partyARotated);

        TradeState trade = (TradeState) accepted.getCoreTransaction().getOutputStates().get(0);
        assertEquals(partyAOriginal, trade.getBuyer());
        assertEquals(partyB, trade.getSeller());
    }

    // --- Assertions --------------------------------------------------------------------------------------------------------

    private static ProposalState outputOf(SignedTransaction stx) {
        return (ProposalState) stx.getCoreTransaction().getOutputStates().get(0);
    }

    private static void assertProposal(ProposalState proposal, Party buyer, Party seller, Party proposer, Party proposee, int amount) {
        assertEquals("buyer", buyer, proposal.getBuyer());
        assertEquals("seller", seller, proposal.getSeller());
        assertEquals("proposer", proposer, proposal.getProposer());
        assertEquals("proposee", proposee, proposal.getProposee());
        assertEquals("amount", amount, proposal.getAmount());
    }

    /**
     * The rotation is proven in the transaction's signatures only: the command carries no proof map, the signature made with
     * {@code rotated}'s key carries a proof chain linking {@code original}'s key to it, and no other signature carries a proof.
     */
    private static void assertRotationProof(SignedTransaction stx, Party original, Party rotated) {
        assertNull("The command should not carry a proof map", stx.getTx().getCommands().get(0).getKeyRotationProofChainMap());

        boolean signedByRotatedKey = false;
        for (TransactionSignature signature : stx.getSigs()) {
            KeyRotationProofChain chain = signature.getSignatureMetadata().getProofChain();
            if (signature.getBy().equals(rotated.getOwningKey())) {
                signedByRotatedKey = true;
                assertNotNull("The signature by the rotated key should carry a proof chain", chain);
                assertTrue("The chain should link the original key to the rotated key", chain.isValid(original.getOwningKey(), rotated.getOwningKey()));
            } else {
                assertTrue("Only the rotated key's signature should carry a proof chain", chain == null || chain.isEmpty());
            }
            assertTrue("The original key must not sign any more", !signature.getBy().equals(original.getOwningKey()));
        }
        assertTrue("The transaction should be signed by " + rotated.getName() + "'s rotated key", signedByRotatedKey);
    }
}
