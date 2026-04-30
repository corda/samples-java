package net.corda.samples.negotiation.contracts;

import com.google.common.collect.ImmutableSet;
import net.corda.core.crypto.keyrotation.crossprovider.PartyIdentityResolver;
import net.corda.samples.negotiation.states.ProposalState;
import net.corda.samples.negotiation.states.TradeState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class ProposalAndTradeContract implements Contract {
    public static String ID = "net.corda.samples.negotiation.contracts.ProposalAndTradeContract";

    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);

        if (command.getValue() instanceof Commands.Propose) {
            // No change is required since the flow creating the transaction will always use the most up-to-date identities
            // when building the transaction.
            requireThat(require -> {
                require.using("There are no inputs", tx.getInputs().isEmpty());
                require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
                require.using("The single output is of type ProposalState", tx.outputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one command", tx.getCommands().size() == 1);
                require.using("There is no timestamp", tx.getTimeWindow() == null);
                ProposalState output = tx.outputsOfType(ProposalState.class).get(0);
                require.using("The buyer and seller are the proposer and the proposee", ImmutableSet.of(output.getBuyer(), output.getSeller()).equals(ImmutableSet.of(output.getProposee(), output.getProposer())));
                require.using("The proposer is a required signer", command.getSigners().contains(output.getProposer().getOwningKey()));
                require.using("The proposee is a required signer", command.getSigners().contains(output.getProposee().getOwningKey()));
                return null;
            });
        } else if (command.getValue() instanceof Commands.Accept) {
            requireThat(require -> {
                require.using("There is exactly one input", tx.getInputStates().size() == 1);
                require.using("The single input is of type ProposalState", tx.inputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one output", tx.getOutputs().size() == 1);
                require.using("The single output is of type TradeState", tx.outputsOfType(TradeState.class).size() == 1);
                require.using("There is exactly one command", tx.getCommands().size() == 1);
                require.using("There is no timestamp", tx.getTimeWindow() == null);

                ProposalState input = tx.inputsOfType(ProposalState.class).get(0);
                TradeState output = tx.outputsOfType(TradeState.class).get(0);

                // Create a resolver using the proof chain map from the command.
                //
                // This allows the resolver to resolve parties across key rotations,
                // ensuring that the same logical parties are identified in the transaction
                // even when their public keys have changed.
                PartyIdentityResolver resolver = new PartyIdentityResolver(command.getKeyRotationProofChainMap());

                require.using("The amount is unmodified in the output", output.getAmount() == input.getAmount());

                // After a key rotation, parties in the input and output states should be compared using the resolver,
                // rather than relying on `equals`, which may fail if a party’s public key has changed.
                //
                // This is only strictly necessary when the flow has been updated to replace the old party with the new one in the output state.
                // The proof chain map will be used by the resolver to determine that the old and new parties are in fact the same, allowing the contract to verify successfully.
                require.using("The buyer is unmodified in the output", resolver.isSameParty(input.getBuyer(), output.getBuyer()));
                require.using("The seller is unmodified in the output", resolver.isSameParty(input.getSeller(), output.getSeller()));

                // Similarly, the required signers should be checked using the resolver to account for any key rotations.
                // The proof chain map will be used by the resolver to determine that the old and new parties are in fact the same, allowing the contract to verify successfully.
                require.using("The proposer is a required signer", resolver.isRequiredSigner(command.getSigners(), input.getProposer()));
                require.using("The proposee is a required signer", resolver.isRequiredSigner(command.getSigners(), input.getProposee()));

                return null;
            });
        } else if (command.getValue() instanceof Commands.Modify) {
            requireThat(require -> {
                require.using("There is exactly one input", tx.getInputStates().size() == 1);
                require.using("The single input is of type ProposalState", tx.inputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one output", tx.getOutputs().size() == 1);
                require.using("The single output is of type ProposalState", tx.outputsOfType(ProposalState.class).size() == 1);
                require.using("There is exactly one command", tx.getCommands().size() == 1);
                require.using("There is no timestamp", tx.getTimeWindow() == null);

                ProposalState input = tx.inputsOfType(ProposalState.class).get(0);
                ProposalState output = tx.outputsOfType(ProposalState.class).get(0);

                // Create a resolver using the proof chain map from the command.
                //
                // This allows the resolver to resolve parties across key rotations,
                // ensuring that the same logical parties are identified in the transaction
                // even when their public keys have changed.
                PartyIdentityResolver resolver = new PartyIdentityResolver(command.getKeyRotationProofChainMap());

                require.using("The amount is unmodified in the output", output.getAmount() != input.getAmount());

                // After a key rotation, parties in the input and output states should be compared using the resolver,
                // rather than relying on `equals`, which may fail if a party’s public key has changed.
                //
                // This is only strictly necessary when the flow has been updated to replace the old party with the new one in the output state.
                // The proof chain map will be used by the resolver to determine that the old and new parties are in fact the same, allowing the contract to verify successfully.
                require.using("The buyer is unmodified in the output", resolver.isSameParty(input.getBuyer(), output.getBuyer()));
                require.using("The seller is unmodified in the output", resolver.isSameParty(input.getSeller(), output.getSeller()));

                // Similarly, the required signers should be checked using the resolver to account for any key rotations.
                // The proof chain map will be used by the resolver to determine that the old and new parties are in fact the same, allowing the contract to verify successfully.
                require.using("The proposer is a required signer", resolver.isRequiredSigner(command.getSigners(), input.getProposer()));
                require.using("The proposee is a required signer", resolver.isRequiredSigner(command.getSigners(), input.getProposee()));

                return null;

            });
        } else {
            throw new IllegalArgumentException("Command of incorrect type");
        }

    }


    public interface Commands extends CommandData {
        class Propose implements Commands {
        }

        ;

        class Accept implements Commands {
        }

        ;

        class Modify implements Commands {
        }

        ;
    }
}
