package com.pr.student.contract.state.schema.contract;

import com.pr.student.contract.state.schema.state.RequestForm;
import com.pr.student.contract.state.schema.state.StudentInfoState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

// ************
// * Contract *
// ************
public class RequestFormContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String REQUEST_CONTRACT_ID = "com.pr.student.contract.state.schema.contract.RequestFormContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());

        if (commandData instanceof Commands.CREATE)
            verifyRequest(tx, setOfSigners);
        else if (commandData instanceof Commands.UPDATE)
            verifyResponse(tx, setOfSigners);
        else
            throw new IllegalArgumentException("Unrecognised command.");


    }

    private Set<PublicKey> keysFromParticipants(RequestForm requestForm) {
        return requestForm
                .getParticipants().stream()
                .map(AbstractParty::getOwningKey)
                .collect(Collectors.toSet());

    }

    private void verifyRequest(LedgerTransaction tx, Set<PublicKey> setOfSigners) {

        requireThat(req -> {
            req.using("No inputs should be consumed when creating PR request.", tx.getInputs().isEmpty());
            req.using("only one Request state should be created.", tx.getOutputs().size() == 1);

            RequestForm requestForm = (RequestForm) tx.getOutputStates().get(0);
            req.using("Signers must be part of participants", setOfSigners.equals(keysFromParticipants(requestForm)));

            return null;
        });
    }

    private void verifyResponse(LedgerTransaction tx, Set<PublicKey> setOfSigners) {

        requireThat(req -> {
            req.using("only one input should be consumed when responding to a Request.",
                    tx.getInputStates().size() == 1);
            req.using("Only one Response state should be created.", tx.getOutputStates().size() == 1);
            RequestForm premium = (RequestForm) tx.getOutputStates().get(0);
            req.using("Signers must be part of participants",
                    setOfSigners.equals(keysFromParticipants(premium)));
            return null;
        });
    }


    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class CREATE extends TypeOnlyCommandData implements Commands {
        }

        class UPDATE extends TypeOnlyCommandData implements Commands {
        }
    }
}