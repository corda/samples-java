package net.corda.samples.duediligence.contracts;

import net.corda.core.identity.CordaX500Name;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.samples.duediligence.contracts.CorporateRecordsContract.CorporateRecordsContract_ID;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.duediligence.contracts")
    );
    public static TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "TestLand", "US"));
    public static TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "TestCity", "US"));

    @Test
    public void ProposeTransactionshouldhavezeroinput() {
        CorporateRecordsAuditRequest cr = new CorporateRecordsAuditRequest(alice.getParty(),bob.getParty(),10);

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(CorporateRecordsContract_ID, cr);
                tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new CorporateRecordsContract.Commands.Propose());
                tx.output(CorporateRecordsContract_ID, cr);
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(alice.getPublicKey(), bob.getPublicKey()), new CorporateRecordsContract.Commands.Propose());
                tx.output(CorporateRecordsContract_ID, cr);
                return tx.verifies(); // As there are no input sates
            });
            return null;
        });
    }
}