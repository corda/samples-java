package net.corda.samples.negotiation.flows;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.crypto.keyrotation.crossprovider.KeyRotationProofChain;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.samples.negotiation.states.ProposalState;
import net.corda.samples.negotiation.states.TradeState;
import org.junit.Test;

import java.security.PublicKey;
import java.util.SortedMap;

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
        assertProposal(outputOf(modifiedByA), partyB, partyARotated, partyARotated, partyB, 2);
        assertProofInCommand(modifiedByA, partyAOriginal, partyARotated);
        assertNoProofInSignatures(modifiedByA);

        // 2. Node B modifies.
        assertNoProof(modify(b, proposalId, 3));

        // 3. Node A modifies again: still no proof.
        assertNoProof(modify(a, proposalId, 4));

        // 4. Node B accepts
        SignedTransaction accepted = accept(b, proposalId);
        assertNoProof(accepted);

        TradeState trade = (TradeState) accepted.getCoreTransaction().getOutputStates().get(0);
        assertEquals(partyARotated, trade.getSeller());
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
        assertNull(commandProofMap(modifiedByB));
        assertProofInSignature(modifiedByB, partyAOriginal, partyARotated);

        // 2. Node A modifies.
        SignedTransaction modifiedByA = modify(a, proposalId, 3);
        assertProposal(outputOf(modifiedByA), partyARotated, partyB, partyARotated, partyB, 3);
        assertProofInCommand(modifiedByA, partyAOriginal, partyARotated);
        assertNoProofInSignatures(modifiedByA);

        // 3. Node B modifies again: still no proof.
        assertNoProof(modify(b, proposalId, 4));

        // 4. Node A accepts
        SignedTransaction accepted = accept(a, proposalId);
        assertNoProof(accepted);

        TradeState trade = (TradeState) accepted.getCoreTransaction().getOutputStates().get(0);
        assertEquals(partyARotated, trade.getBuyer());
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

    private static SortedMap<PublicKey, KeyRotationProofChain> commandProofMap(SignedTransaction stx) {
        return stx.getTx().getCommands().get(0).getKeyRotationProofChainMap();
    }

    /** The command's proof map links {@code before}'s key to {@code after}'s key. */
    private static void assertProofInCommand(SignedTransaction stx, Party before, Party after) {
        SortedMap<PublicKey, KeyRotationProofChain> proofMap = commandProofMap(stx);
        assertNotNull("The command should carry a proof map", proofMap);
        KeyRotationProofChain chain = proofMap.get(before.getOwningKey());
        assertNotNull("The proof map should have a chain for the old key", chain);
        assertTrue("The chain should link the old key to the new key", chain.isValid(before.getOwningKey(), after.getOwningKey()));
    }

    /** The signature made with {@code after}'s key carries a proof chain linking {@code before}'s key to it. */
    private static void assertProofInSignature(SignedTransaction stx, Party before, Party after) {
        for (TransactionSignature signature : stx.getSigs()) {
            if (signature.getBy().equals(after.getOwningKey())) {
                KeyRotationProofChain chain = signature.getSignatureMetadata().getProofChain();
                assertNotNull("The signature by the new key should carry a proof chain", chain);
                assertTrue("The chain should link the old key to the new key", chain.isValid(before.getOwningKey(), after.getOwningKey()));
                return;
            }
        }
        throw new AssertionError("No signature by " + after.getName() + "'s new key");
    }

    private static void assertNoProofInSignatures(SignedTransaction stx) {
        for (TransactionSignature signature : stx.getSigs()) {
            KeyRotationProofChain chain = signature.getSignatureMetadata().getProofChain();
            assertTrue("No signature should carry a proof chain", chain == null || chain.isEmpty());
        }
    }

    /** Neither the command nor any signature carries a proof: the transaction looks exactly like one without any rotation. */
    private static void assertNoProof(SignedTransaction stx) {
        assertNull("The command should not carry a proof map", commandProofMap(stx));
        assertNoProofInSignatures(stx);
    }
}
