package net.corda.sample.snl.contracts;

import net.corda.core.identity.CordaX500Name;
import net.corda.sample.snl.contracts.BoardConfigContract;
import net.corda.sample.snl.states.BoardConfig;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class ContractTests {
    private final TestIdentity p1 = new TestIdentity(new CordaX500Name("PL1", "", "IN"));
    private final TestIdentity p2 = new TestIdentity(new CordaX500Name("PL2", "", "IN"));
    private MockServices ledgerServices = new MockServices(new TestIdentity(new CordaX500Name("TestId", "", "IN")));

    private BoardConfig boardConfig_f = new BoardConfig(null, null, Arrays.asList(p1.getParty(), p2.getParty()));

    private BoardConfig boardConfig_s = new BoardConfig(new LinkedHashMap(Collections.singletonMap(1, 5)),
            new LinkedHashMap(Collections.singletonMap(1, 5)), Arrays.asList(p1.getParty(), p2.getParty()));

    @Test
    public void testVerifyCreateBoardZeroInputs() {
        transaction(ledgerServices, tx -> {
            // Has an input, will fail.
            tx.input(BoardConfigContract.ID, boardConfig_s);
            tx.output(BoardConfigContract.ID, boardConfig_s);
            tx.command(Arrays.asList(p1.getPublicKey(), p2.getPublicKey()), new BoardConfigContract.Commands.Create());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has no input, will verify.
            tx.output(BoardConfigContract.ID, boardConfig_s);
            tx.command(Arrays.asList(p1.getPublicKey(), p2.getPublicKey()), new BoardConfigContract.Commands.Create());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void testVerifyCreateBoardNonEmptySnakeAndLadderPositions() {
        transaction(ledgerServices, tx -> {
            // Has empty/ null snake and ladder positions, should fail
            tx.output(BoardConfigContract.ID, boardConfig_f);
            tx.command(Arrays.asList(p1.getPublicKey(), p2.getPublicKey()), new BoardConfigContract.Commands.Create());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has no input, will verify.
            tx.output(BoardConfigContract.ID, boardConfig_s);
            tx.command(Arrays.asList(p1.getPublicKey(), p2.getPublicKey()), new BoardConfigContract.Commands.Create());
            tx.verifies();
            return null;
        });
    }

}