package net.corda.samples.sendfile.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.samples.sendfile.contracts.InvoiceContract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(InvoiceContract.class)
public class InvoiceState implements ContractState {
    private final String invoiceAttachmentID;
    private final List<AbstractParty> participants;

    public InvoiceState(String invoiceAttachmentID, List<AbstractParty> participants) {
        this.invoiceAttachmentID = invoiceAttachmentID;
        this.participants = participants;
    }

    @NotNull
    public String getInvoiceAttachmentID() {
        return invoiceAttachmentID;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return participants;
    }
}
