package net.corda.samples.oracle.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.oracle.states.PrimeState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;
import static org.jgroups.util.Util.assertEquals;

public class PrimeContractTests {

    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands {
        }
    }

    private MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.oracle.contracts")
    );

    private TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));
    private PrimeState st = new PrimeState(1, 5, a.getParty());

    @Test
    public void contractImplementsContract() {
        assert (new PrimeContract() instanceof Contract);
    }

    @Test
    public void constructorTest() {

        assertEquals(1, st.getN());
        assertEquals(5, st.getNthPrime());
    }

    @Test
    public void contractRequiresOneCommandInTheTransaction() {
        transaction(ledgerServices, tx -> {
            tx.output(PrimeContract.PRIME_PROGRAM_ID, st);
            // Has two commands, will fail.
            tx.command(Arrays.asList(a.getPublicKey()), new PrimeContract.Commands.Create(1, 5));
            tx.command(Arrays.asList(a.getPublicKey()), new PrimeContract.Commands.Create(1, 5));
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(PrimeContract.PRIME_PROGRAM_ID, st);
            // Has one command, will verify.
            tx.command(Arrays.asList(a.getPublicKey()), new PrimeContract.Commands.Create(1, 5));
            tx.verifies();
            return null;
        });
    }

    @Test
    public void contractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        transaction(ledgerServices, tx -> {
            // Has wrong command type, will fail.
            tx.output(PrimeContract.PRIME_PROGRAM_ID, st);
            tx.command(Arrays.asList(a.getPublicKey()), DummyCommandData.INSTANCE);
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct command type, will verify.
            tx.output(PrimeContract.PRIME_PROGRAM_ID, st);
            tx.command(Arrays.asList(a.getPublicKey()), new PrimeContract.Commands.Create(1, 5));
            tx.verifies();
            return null;
        });
    }

}
