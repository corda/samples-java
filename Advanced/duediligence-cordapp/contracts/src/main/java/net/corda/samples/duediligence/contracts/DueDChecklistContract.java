package net.corda.samples.duediligence.contracts;

import net.corda.core.contracts.*;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.duediligence.states.CopyOfCoporateRecordsAuditRequest;
import net.corda.samples.duediligence.states.CorporateRecordsAuditRequest;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class DueDChecklistContract implements Contract {
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);

        //Propose request
        if (command.getValue() instanceof DueDChecklistContract.Commands.Add) {
            requireThat(require -> {
                StateAndRef<ContractState> input = tx.getInputs().get(0);
                if (input.getState().getData().getClass() == CorporateRecordsAuditRequest.class){
                    CorporateRecordsAuditRequest request = (CorporateRecordsAuditRequest)input.getState().getData();
                    require.using("Qualification Must be True", request.getQualification());
                }else{
                    CopyOfCoporateRecordsAuditRequest request = (CopyOfCoporateRecordsAuditRequest) input.getState().getData();
                    require.using("Qualification Must be True", request.getQualification());
                }
                return null;
            });
        }
    }

    public interface Commands extends CommandData {
        class Add implements Commands { }
    }
}
