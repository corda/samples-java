package com.template.contracts;

import com.template.states.Avatar;
import com.template.states.Expiry;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class AvatarContract implements Contract {

    public static final String AVATAR_CONTRACT_ID = "com.template.contracts.AvatarContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        CommandWithParties<Commands> commandWithParties = requireSingleCommand(tx.getCommands(), Commands.class);
        Commands value = commandWithParties.getValue();
        List<PublicKey> signers = commandWithParties.getSigners();

        if (value instanceof Commands.Create) {
            requireThat(require -> {
                require.using("There should be 0 input states.", tx.getInputs().isEmpty());
                require.using("There should be 2 output states.", tx.getOutputStates().size() == 2);
                require.using("There should be 1 expiry state.", tx.outputsOfType(Expiry.class).size() == 1);
                require.using("There shoule be 1 Avatar created.", tx.outputsOfType(Avatar.class).size() == 1);

                Avatar avatar = tx.outputsOfType(Avatar.class).get(0);
                require.using("Avatar Owner must always sign the newly created Avatar.", signers.contains(avatar.getOwner().getOwningKey()));

                Integer avatarEncumbrance = tx.getOutputs().stream().filter(o -> o.getData() instanceof Avatar).findFirst().get().getEncumbrance();
                require.using("Avatar needs to be encumbered", avatarEncumbrance != null);

                return null;
            });
        } else if (value instanceof Commands.Transfer) {
            requireThat(require -> {
                require.using("There should be 2 inputs.", tx.getInputs().size() == 2);
                require.using("There must be 1 expiry as an input.", tx.inputsOfType(Expiry.class).size() == 1);
                require.using("There must be 1 avatar as an input", tx.inputsOfType(Avatar.class).size() == 1);

                require.using("There should be two output states", tx.getInputs().size() == 2);
                require.using("There should be 1 expiry state.", tx.outputsOfType(Expiry.class).size() == 1);
                require.using("There shoule be 1 Avatar created.", tx.outputsOfType(Avatar.class).size() == 1);

                Avatar newAvatar = tx.outputsOfType(Avatar.class).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("No Avatar created for transferring."));
                Avatar oldAvatar = tx.inputsOfType(Avatar.class).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Existing Avatar to transfer not found."));

                require.using("New and old Avatar must just have the owners changed.", newAvatar.equals(oldAvatar));
                require.using("New Owner should sign the new Avatar", signers.contains(newAvatar.getOwner().getOwningKey()));
                require.using("Old owner must sign the old Avatar", signers.contains(oldAvatar.getOwner().getOwningKey()));

                return null;
            });

        }
    }

    public interface Commands extends CommandData {
        class Create implements Commands { }

        class Transfer implements Commands { }
    }
}
