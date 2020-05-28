package net.corda.samples.contract;


import net.corda.core.contracts.*;
import net.corda.finance.*;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.node.MockServices;

import static net.corda.testing.node.NodeTestUtils.ledger;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.samples.TestUtils.*;

import net.corda.samples.contracts.IOUContract;
import net.corda.samples.state.IOUStateTests;

import java.util.Arrays;

import net.corda.samples.states.IOUState;
import org.junit.*;


/**
 * Practical exercise instructions for Contracts Part 1.
 * The objective here is to write some contract code that verifies a transaction to issue an {@link IOUState}.
 * As with the {@link IOUStateTests} uncomment each unit test and run them one at a time. Use the body of the tests and the
 * task description to determine how to get the tests to pass.
 */
public class IOUIssueTests {
    // A pre-defined dummy command.
    public interface Commands extends CommandData {
        class DummyCommand extends TypeOnlyCommandData implements Commands{}
    }

    static private final MockServices ledgerServices = new MockServices(
            Arrays.asList("net.corda.samples", "net.corda.finance.contracts")
    );

    /**
     * Task 1.
     * Recall that Commands are required to hint to the intention of the transaction as well as take a list of
     * public keys as parameters which correspond to the required signers for the transaction.
     * Commands also become more important later on when multiple actions are possible with an IOUState, e.g. Transfer
     * and Settle.
     * TODO: Add an "Issue" command to the IOUContract and check for the existence of the command in the verify function.
     * Hint:
     * - For the Issue command we only care about the existence of it in a transaction, therefore it should extend
     *   the {@link TypeOnlyCommandData} class.
     * - The command should be defined inside {@link IOUContract}.
     * - We usually encapsulate our commands in an interface inside the contract class called {@link Commands} which
     *   extends the {@link CommandData} interface. The Issue command itself should be defined inside the {@link Commands}
     *   interface as well as implement it, for example:
     *
     *   public interface Commands extends CommandData {
     *      class X extends TypeOnlyCommandData implements Commands{}
     *   }
     *
     * - We can check for the existence of any command that implements [IOUContract.Commands] by using the
     *   [requireSingleCommand] function which takes a {@link Class} argument.
     * - You can use the [requireSingleCommand] function on [tx.getCommands()] to check for the existence and type of the specified command
     *   in the transaction. [requireSingleCommand] requires a Class argument to identify the type of command required.
     *
     *         requireSingleCommand(tx.getCommands(), REQUIRED_COMMAND.class)
     */
    @Test
    public void mustIncludeIssueCommand() {
        IOUState iou = new IOUState(Currencies.POUNDS(1), ALICE.getParty(), BOB.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new Commands.DummyCommand()); // Wrong type.
                return tx.failsWith("Contract verification failed");
            });
            l.transaction(tx -> {
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue()); // Correct type.
                return tx.verifies();
            });
            return null;
        });
    }

    /**
     * Task 2.
     * As previously observed, issue transactions should not have any input state references. Therefore we must check to
     * ensure that no input states are included in a transaction to issue an IOU.
     * TODO: Write a contract constraint that ensures a transaction to issue an IOU does not include any input states.
     * Hint: use a [requireThat] lambda with a constraint to inside the [IOUContract.verify] function to encapsulate your
     * constraints:
     *
     *     requireThat(requirement -> {
     *          requirement.using("Message when constraint fails", (boolean constraining expression));
     *          // passes all cases
     *          return null;
     *     });
     *
     * Note that the unit tests often expect contract verification failure with a specific message which should be
     * defined with your contract constraints. If not then the unit test will fail!
     *
     * You can access the list of inputs via the {@link LedgerTransaction} object which is passed into
     * [IOUContract.verify].
     */
    @Test
    public void issueTransactionMustHaveNoInputs() {
        IOUState iou = new IOUState(Currencies.POUNDS(1), ALICE.getParty(), BOB.getParty());

        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.input(IOUContract.IOU_CONTRACT_ID, new DummyState());
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.failsWith("No inputs should be consumed when issuing an IOU");
            });
            l.transaction(tx -> {
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                return tx.verifies(); // As there are no input sates
            });
            return null;
        });
    }

    /**
     * Task 3.
     * Now we need to ensure that only one {@link IOUState} is issued per transaction.
     * TODO: Write a contract constraint that ensures only one output state is created in a transaction.
     * Hint: Write an additional constraint within the existing [requireThat] block which you created in the previous
     * task.
     */
    @Test
    public void issueTransactionMustHaveOneOutput() {
        IOUState iou = new IOUState(Currencies.POUNDS(1), ALICE.getParty(), BOB.getParty());
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou); // Two outputs fails.
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.failsWith("Only one output state should be created when issuing an IOU.");
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou); // One output passes.
                return tx.verifies();
            });
            return null;
        });
    }

    /**
     * Task 4.
     * Now we need to consider the properties of the {@link IOUState}. We need to ensure that an IOU should always have a
     * positive value.
     * TODO: Write a contract constraint that ensures newly issued IOUs always have a positive value.
     * Hint: You will need a number of hints to complete this task!
     * - Create a new constant which will hold a reference to the output IOU state.
     * - We need to obtain a reference to the proposed IOU for issuance from the [LedgerTransaction.getOutputStates()] list
     * - You can use the function [get(0)] to grab the single element from the list.
     * This list is typed as a list of {@link ContractState}s, therefore we need to cast the {@link ContractState} which we return
     *   to an {@link IOUState}. E.g.
     *
     *       XState state = (XState)tx.getOutputStates().get(0)
     *
     * - When checking the [IOUState.getAmount()] property is greater than zero, you need to check the
     *   [IOUState.getAmount().getQuantity()] field.
     */
    @Test
    public void cannotCreateZeroValueIOUs() {
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, new IOUState(Currencies.POUNDS(0), ALICE.getParty(), BOB.getParty())); // Zero amount fails.
                return tx.failsWith("A newly issued IOU must have a positive amount.");
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, new IOUState(Currencies.SWISS_FRANCS(100), ALICE.getParty(), BOB.getParty()));
                return tx.verifies();
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, new IOUState(Currencies.POUNDS(1), ALICE.getParty(), BOB.getParty()));
                return tx.verifies();
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, new IOUState(Currencies.DOLLARS(10), ALICE.getParty(), BOB.getParty()));
                return tx.verifies();
            });
            return null;
        });
    }

    /**
     * Task 5.
     * For obvious reasons, the identity of the lender and borrower must be different.
     * TODO: Add a contract constraint to check the lender is not the borrower.
     * Hint:
     * - You can use the [IOUState.getLender()] and [IOUState.getBorrower()] properties.
     * - This check must be made before the checking who has signed.
     */
    @Test
    public void lenderAndBorrowerCannotBeTheSame() {
        IOUState iou = new IOUState(Currencies.POUNDS(1), ALICE.getParty(), BOB.getParty());
        IOUState borrowerIsLenderIou = new IOUState(Currencies.POUNDS(10), ALICE.getParty(), ALICE.getParty());
        ledger(ledgerServices, l-> {
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, borrowerIsLenderIou);
                return tx.failsWith("The lender and borrower cannot have the same identity.");
            });
            l.transaction(tx -> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.verifies();
            });
            return null;
        });
    }

    /**
     * Task 6.
     * The list of public keys which the commands hold should contain all of the participants defined in the {@link IOUState}.
     * This is because the IOU is a bilateral agreement where both parties involved are required to sign to issue an
     * IOU or change the properties of an existing IOU.
     * TODO: Add a contract constraint to check that all the required signers are {@link IOUState} participants.
     * Hint:
     * - In Java, you can perform a set equality check of two sets with the .equals()
     * - We need to check that the signers for the Command of this transaction equals the participants list.
     * - We don't want any additional public keys not listed in the IOUs participants list.
     * - You will need a reference to the Issue command to get access to the list of signers.
     * - [requireSingleCommand] returns the single required [CommandWithParties<Commands>] - you can assign the return
     * value to a constant.
     * - Next, you will need to retrieve the participants of the output state and ensure they are equal them.
     *
     * Java Hints
     * - Java's map function allows for conversion of a Collection. However, it requires a Stream object (created by
     * calling collection.stream()), which must then
     * be converted back into a Collection using collect(Collectors.toCOLLECTION_TYPE). All together, this looks like:
     *      collection.stream().map(element -> (some operation on element)).collect(Collectors.toCOLLECTION_TYPE)
     * This will be needed for mapping the List<Party> from getParticipants() to a List<PublicKey>
     * - https://zeroturnaround.com/rebellabs/java-8-explained-applying-lambdas-to-java-collections/
     * - A Collection can be turned into a set using: new HashSet<>(collection)
//     */
    @Test
    public void lenderAndBorrowerMustSignIssueTransaction() {
        IOUState iou = new IOUState(Currencies.POUNDS(1), ALICE.getParty(), BOB.getParty());
        ledger(ledgerServices, l->{
            l.transaction(tx-> {
                tx.command(DUMMY.getPublicKey(),  new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.failsWith("Both lender and borrower together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(ALICE.getPublicKey(),  new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.failsWith("Both lender and borrower together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(BOB.getPublicKey(),  new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.failsWith("Both lender and borrower together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(BOB.getPublicKey(), BOB.getPublicKey(), BOB.getPublicKey()),  new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.failsWith("Both lender and borrower together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(BOB.getPublicKey(), BOB.getPublicKey(), MINICORP.getPublicKey(), ALICE.getPublicKey()),  new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.failsWith("Both lender and borrower together only may sign IOU issue transaction.");
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(BOB.getPublicKey(), BOB.getPublicKey(), BOB.getPublicKey(), ALICE.getPublicKey()),  new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.verifies();
            });
            l.transaction(tx-> {
                tx.command(Arrays.asList(ALICE.getPublicKey(), BOB.getPublicKey()), new IOUContract.Commands.Issue());
                tx.output(IOUContract.IOU_CONTRACT_ID, iou);
                return tx.verifies();
            });
            return null;
        });
    }
}