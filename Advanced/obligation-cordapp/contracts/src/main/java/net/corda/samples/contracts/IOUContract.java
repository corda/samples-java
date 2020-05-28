package net.corda.samples.contracts;

import net.corda.core.contracts.*;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;
import net.corda.samples.states.IOUState;

import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the contract code which defines how the [IOUState] behaves. Looks at the unit tests in
 * [IOUContractTests] for more insight on how this contract verifies a transaction.
 */

// LegalProseReference: this is just a dummy string for the time being.

@LegalProseReference(uri = "<prose_contract_uri>")
public class IOUContract implements Contract {
    public static final String IOU_CONTRACT_ID = "net.corda.samples.contracts.IOUContract";

    /**
     * The IOUContract can handle three transaction types involving [IOUState]s.
     * - Issuance: Issuing a new [IOUState] on the ledger, which is a bilateral agreement between two parties.
     * - Transfer: Re-assigning the lender/beneficiary.
     * - Settle: Fully or partially settling the [IOUState] using the Corda [Cash] contract.
     */
    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands{}
        class Transfer extends TypeOnlyCommandData implements Commands{}
        class Settle extends TypeOnlyCommandData implements Commands{}
    }
    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    @Override
    public void verify(LedgerTransaction tx) {

        // We can use the requireSingleCommand function to extract command data from transaction.
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        /**
         * This command data can then be used inside of a conditional statement to indicate which set of tests we
         * should be performing - we will use different assertions to enable the contract to verify the transaction
         * for issuing, settling and transferring.
         */
        if (commandData.equals(new Commands.Issue())) {

            requireThat(require -> {

                require.using("No inputs should be consumed when issuing an IOU.", tx.getInputStates().size() == 0);
                require.using( "Only one output state should be created when issuing an IOU.", tx.getOutputStates().size() == 1);

                IOUState outputState = tx.outputsOfType(IOUState.class).get(0);
                require.using( "A newly issued IOU must have a positive amount.", outputState.amount.getQuantity() > 0);
                require.using( "The lender and borrower cannot have the same identity.", outputState.lender.getOwningKey() != outputState.borrower.getOwningKey());

                List<PublicKey> signers = tx.getCommands().get(0).getSigners();
                HashSet<PublicKey> signersSet = new HashSet<>();
                for (PublicKey key: signers) {
                    signersSet.add(key);
                }

                List<AbstractParty> participants = tx.getOutputStates().get(0).getParticipants();
                HashSet<PublicKey> participantKeys = new HashSet<>();
                for (AbstractParty party: participants) {
                    participantKeys.add(party.getOwningKey());
                }

                require.using("Both lender and borrower together only may sign IOU issue transaction.", signersSet.containsAll(participantKeys) && signersSet.size() == 2);

                return null;
            });

        }

        else if (commandData.equals(new Commands.Transfer())) {

            requireThat(require -> {

                require.using("An IOU transfer transaction should only consume one input state.", tx.getInputStates().size() == 1);
                require.using("An IOU transfer transaction should only create one output state.", tx.getOutputStates().size() == 1);

                // Copy of input with new lender;
                IOUState inputState = tx.inputsOfType(IOUState.class).get(0);
                IOUState outputState = tx.outputsOfType(IOUState.class).get(0);
                IOUState checkOutputState = outputState.withNewLender(inputState.getLender());

                require.using("Only the lender property may change.",
                        checkOutputState.amount.equals(inputState.amount) && checkOutputState.getLinearId().equals(inputState.getLinearId()) && checkOutputState.borrower.equals(inputState.borrower) && checkOutputState.paid.equals(inputState.paid));
                require.using("The lender property must change in a transfer.", !outputState.lender.getOwningKey().equals(inputState.lender.getOwningKey()));

                List<PublicKey> listOfPublicKeys = new ArrayList<>();
                listOfPublicKeys.add(inputState.lender.getOwningKey());
                listOfPublicKeys.add(inputState.borrower.getOwningKey());
                listOfPublicKeys.add(checkOutputState.lender.getOwningKey());

                Set<PublicKey> listOfParticipantPublicKeys = inputState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toSet());
                listOfParticipantPublicKeys.add(outputState.lender.getOwningKey());
                List<PublicKey> arrayOfSigners = command.getSigners();
                Set<PublicKey> setOfSigners = new HashSet<PublicKey>(arrayOfSigners);
                require.using("The borrower, old lender and new lender only must sign an IOU transfer transaction", setOfSigners.equals(listOfParticipantPublicKeys) && setOfSigners.size() == 3);
                return null;

            });

        }

        else if (commandData.equals(new Commands.Settle())) {

            requireThat(require -> {

                // Check there is only one group of IOUs and that there is always an input IOU.
                List<LedgerTransaction.InOutGroup<IOUState, UniqueIdentifier>> groups = tx.groupStates(IOUState.class, IOUState::getLinearId);
                require.using("There must be one input IOU.", groups.get(0).getInputs().size() > 0);

                // Check that there are output cash states.
                List<Cash.State> allOutputCash = tx.outputsOfType(Cash.State.class);
                require.using("There must be output cash.", !allOutputCash.isEmpty());

                // Check that there is only one group of input IOU's
                List<LedgerTransaction.InOutGroup<IOUState, UniqueIdentifier>> allGroupStates = tx.groupStates(IOUState.class, IOUState::getLinearId);
                require.using("List has more than one element.", allGroupStates.size() < 2);

                IOUState inputIOU = tx.inputsOfType(IOUState.class).get(0);
                Amount<Currency> inputAmount = inputIOU.amount;

                // check that the output cash is being assigned to the lender
                Party lenderIdentity = inputIOU.lender;
                List<Cash.State> acceptableCash = allOutputCash.stream().filter(cash -> cash.getOwner().getOwningKey().equals(lenderIdentity.getOwningKey())).collect(Collectors.toList());

                require.using("There must be output cash paid to the recipient.", acceptableCash.size() > 0);

                // Sum the acceptable cash sent to the lender
                Amount<Currency> acceptableCashSum = new Amount<>(0, inputAmount.getToken());
                for (Cash.State cash: acceptableCash) {
                    Amount<Currency> addCash = new Amount<>(cash.getAmount().getQuantity(), cash.getAmount().getToken().getProduct());
                    acceptableCashSum = acceptableCashSum.plus(addCash);
                }

                Amount<Currency> amountOutstanding = inputIOU.amount.minus(inputIOU.paid);
                require.using("The amount settled cannot be more than the amount outstanding.", amountOutstanding.getQuantity() >= acceptableCashSum.getQuantity());

                if (amountOutstanding.equals(acceptableCashSum)) {
                    // If the IOU has been fully settled then there should be no IOU output state.
                    require.using("There must be no output IOU as it has been fully settled.", tx.outputsOfType(IOUState.class).isEmpty());

                } else {
                    // If the IOU has been partially settled then it should still exist.
                    require.using("There must be one output IOU.", tx.outputsOfType(IOUState.class).size() == 1);

                    IOUState outputIOU = tx.outputsOfType(IOUState.class).get(0);

                    require.using("The amount may not change when settling.", inputIOU.amount.equals(outputIOU.amount));
                    require.using("The lender may not change when settling.", inputIOU.lender.equals(outputIOU.lender));
                    require.using("The borrower may not change when settling.", inputIOU.borrower.equals(outputIOU.borrower));
                }

                Set<PublicKey> listOfParticipantPublicKeys = inputIOU.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toSet());
                List<PublicKey> arrayOfSigners = command.getSigners();
                Set<PublicKey> setOfSigners = new HashSet<PublicKey>(arrayOfSigners);
                require.using("Both lender and borrower must sign IOU settle transaction.", setOfSigners.equals(listOfParticipantPublicKeys));

                return null;
            });

        }

    }

}