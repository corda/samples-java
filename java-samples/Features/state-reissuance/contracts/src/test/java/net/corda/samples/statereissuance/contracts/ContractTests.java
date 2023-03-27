package net.corda.samples.statereissuance.contracts;

import net.corda.samples.statereissuance.states.LandTitleState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.ledger;


public class ContractTests {
    private final MockServices ledgerServices = new MockServices(Arrays.asList("net.corda.samples.statereissuance"));
    TestIdentity alice = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));
    TestIdentity bob = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));


}