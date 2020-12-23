package net.corda.samples.heartbeat.contracts;


import net.corda.core.contracts.Contract;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.heartbeat.states.HeartState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class ContractTests {

    private final TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private final TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));

    private MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.heartbeat.contracts")
    );

    HeartState st = new HeartState(a.getParty());

    @Test
    public void contractImplementsContract() {
        assert (new HeartContract() instanceof Contract);
    }

    @Test
    public void contractRequiresSpecificCommand() {
        transaction(ledgerServices, tx -> {
            // Has correct command type, will verify.
            tx.output(HeartContract.contractID, st);
            tx.command(Arrays.asList(a.getPublicKey()), new HeartContract.Commands.Beat());
            tx.verifies();
            return null;
        });
    }

}
