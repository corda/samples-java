package net.corda.samples.whistleblower.contracts;

import net.corda.core.contracts.Contract;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.whistleblower.states.BlowWhistleState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class BlowWhistleContractTests {

    private final TestIdentity a = new TestIdentity(new CordaX500Name("alice", "", "GB"));
    private final TestIdentity b = new TestIdentity(new CordaX500Name("bob", "", "GB"));
    private final TestIdentity c = new TestIdentity(new CordaX500Name("bad corp", "", "GB"));

    private MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.whistleblower.contracts")
    );

    BlowWhistleState st = new BlowWhistleState(c.getParty(), a.getParty().anonymise(), b.getParty().anonymise());

    @Test
    public void SantaSessionContractImplementsContract() {
        assert (new BlowWhistleContract() instanceof Contract);
    }

    @Test
    public void contractRequiresZeroInputsInTheTransaction() {

        transaction(ledgerServices, tx -> {
            // Has an input, will fail.
            tx.output(BlowWhistleContract.ID, st);
            tx.output(BlowWhistleContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has no input, will verify.
            tx.output(BlowWhistleContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void contractRequiresSpecificCommand() {
        transaction(ledgerServices, tx -> {
            // Has wrong command type, will fail.
            tx.output(BlowWhistleContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey()), DummyCommandData.INSTANCE);
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct command type, will verify.
            tx.output(BlowWhistleContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
            tx.verifies();
            return null;
        });

    }

}
