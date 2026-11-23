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

/**
 * The CorDapp keeps working after Node A rotates its key across providers (Corda Enterprise).
 *
 * A rotation proof is only needed the first time a state that names the OLD key is consumed, and where it travels depends on
 * who starts that transaction:
 *
 * - If the node that rotated starts it, it replaces its old key by the new one in the output and puts the proof in the
 *   command (see {@code generateProofChainMap} in {@code ModificationFlow}).
 * - If the counterparty starts it, it does not know the rotation yet, so the output keeps the old key and the node that
 *   rotated proves the rotation in its signature instead. From then on the counterparty knows the proof too.
 *
 * Once every state names only current keys, no proof appears anywhere and the flows run exactly as before the rotation.
 *
 * In this sample only the proposee can modify or accept a proposal, and whoever modifies becomes the new proposer, so the two
 * tests below start from a proposal made by the node that must NOT start the first transaction after the rotation.
 */
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

    @Test
    public void nodeARotatesAndNodeBReplacesTheKeyWithALearnedProof() throws Exception {
        Party partyAOriginal = identityOf(a);
        Party partyB = identityOf(b);

        // Node A proposes twice to Node B. Only the proposee can modify, so Node B needs two proposals to act twice.
        UniqueIdentifier firstProposalId = propose(a, 1, partyB);
        UniqueIdentifier secondProposalId = propose(a, 1, partyB);

        a = rotate(a);
        Party partyARotated = identityOf(a);

        // 1. Node B modifies the first proposal: Node A proves its rotation in its signature, and Node B learns the proof.
        SignedTransaction firstModifiedByB = modify(b, firstProposalId, 2);
        assertProposal(outputOf(firstModifiedByB), partyAOriginal, partyB, partyB, partyAOriginal, 2);
        assertNull(commandProofMap(firstModifiedByB));
        assertProofInSignature(firstModifiedByB, partyAOriginal, partyARotated);

        // 2. Node B modifies the second proposal: it resolves Node A to its rotated key with the learned proof, names that key
        // in the output and puts Node A's proof in the command.
        SignedTransaction secondModifiedByB = modify(b, secondProposalId, 2);
        assertProposal(outputOf(secondModifiedByB), partyARotated, partyB, partyB, partyARotated, 2);
        assertProofInCommand(secondModifiedByB, partyAOriginal, partyARotated);
        assertNoProofInSignatures(secondModifiedByB);
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
