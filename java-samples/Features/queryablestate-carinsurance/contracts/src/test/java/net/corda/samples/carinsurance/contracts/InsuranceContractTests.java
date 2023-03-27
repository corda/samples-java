package net.corda.samples.carinsurance.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.samples.carinsurance.states.Claim;
import net.corda.samples.carinsurance.states.InsuranceState;
import net.corda.samples.carinsurance.states.VehicleDetail;
import net.corda.testing.core.DummyCommandData;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;

import static net.corda.testing.node.NodeTestUtils.transaction;

public class InsuranceContractTests {

    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands {
        }
    }

    private MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples.carinsurance.contracts")
    );

    private TestIdentity a = new TestIdentity(new CordaX500Name("Alice", "", "GB"));
    private TestIdentity b = new TestIdentity(new CordaX500Name("Bob", "", "GB"));

    String registrationNumber = "registration number: 2ds9Fvk";
    String chassisNum = "chassis# aedl3sc";
    String make = "Toyota";
    String model = "Corolla";
    String variant = "SE";
    String color = "hot rod beige";
    String fuelType = "regular";

    VehicleDetail vd = new VehicleDetail(
            registrationNumber,
            chassisNum,
            make,
            model,
            variant,
            color,
            fuelType);

    String desc = "claim description: my car was hit by a blockchain";
    String claimNumber = "B-132022";
    int claimAmount = 3000;

    Claim c = new Claim(claimNumber, desc, claimAmount);

    // in this test scenario, alice is our insurer.
    String policyNum = "R3-Policy-A4byCd";
    long insuredValue = 100000L;
    int duration = 50;
    int premium = 5;
    Party insurer = a.getParty();
    Party insuree = b.getParty();

    InsuranceState st = new InsuranceState(
            policyNum,
            insuredValue,
            duration,
            premium,
            insurer,
            insuree,
            vd,
            Arrays.asList(c));

    @Test
    public void contractImplementsContract() {
        assert (new InsuranceContract() instanceof Contract);
    }

    @Test
    public void contractRequiresOneCommandInTheTransaction() {
        transaction(ledgerServices, tx -> {
            tx.output(InsuranceContract.ID, st);
            // Has two commands, will fail.
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InsuranceContract.Commands.IssueInsurance());
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InsuranceContract.Commands.IssueInsurance());
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            tx.output(InsuranceContract.ID, st);
            // Has one command, will verify.
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InsuranceContract.Commands.IssueInsurance());
            tx.verifies();
            return null;
        });
    }

    @Test
    public void contractRequiresTheTransactionsCommandToBeAnIssueCommand() {
        transaction(ledgerServices, tx -> {
            // Has wrong command type, will fail.
            tx.output(InsuranceContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey()), DummyCommandData.INSTANCE);
            tx.fails();
            return null;
        });

        transaction(ledgerServices, tx -> {
            // Has correct command type, will verify.
            tx.output(InsuranceContract.ID, st);
            tx.command(Arrays.asList(a.getPublicKey(), b.getPublicKey()), new InsuranceContract.Commands.IssueInsurance());
            tx.verifies();
            return null;
        });
    }
}
