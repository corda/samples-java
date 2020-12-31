package net.corda.samples.sendfile.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.sendfile.states.InvoiceState;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;
import static org.jgroups.util.Util.assertEquals;

public class InvoiceContractTests {

    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands {
        }
    }

    private MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.sendfile.contracts")
    );

    private TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));

    private final String STRINGID = "StringID that is unique";
    private InvoiceState st = new InvoiceState(STRINGID, Arrays.asList(a.getParty(), b.getParty()));


    @Test
    public void constructorTest() {
        assertEquals(STRINGID, st.getInvoiceAttachmentID());
    }

    @Test
    public void InvoiceContractImplementsContract() {
        assert (new InvoiceContract() instanceof Contract);
    }

    @Test
    public void contractRequiresOneCommandInTheTransaction() {
        transaction(ledgerServices, tx -> {
            tx.output(InvoiceContract.ID, st);
            // Has two commands, will fail.
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InvoiceContract.Commands.Issue());
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InvoiceContract.Commands.Issue());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(InvoiceContract.ID, st);
            // Has one command, will verify.
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InvoiceContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void contractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        transaction(ledgerServices, tx -> {
            // Has wrong command type, will fail.
            tx.output(InvoiceContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey()), DummyCommandData.INSTANCE);
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct command type, will verify.
            tx.output(InvoiceContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InvoiceContract.Commands.Issue());
            tx.verifies();
            return null;
        });
    }

}
