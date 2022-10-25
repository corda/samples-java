package net.corda.samples.multioutput.states;

import net.corda.samples.multioutput.contracts.OmniCountContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(OmniCountContract.class)
public class OmniCountState implements ContractState, LinearState {

    //private variables
    private Integer omniAmount;
    private Party borrowerHost;
    private Party settler;
    private UniqueIdentifier linearId;

    public OmniCountState(Integer omniAmount, Party borrowerHost, Party settler, UniqueIdentifier linearId) {
        this.omniAmount = omniAmount;
        this.borrowerHost = borrowerHost;
        this.settler = settler;
        this.linearId = linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(borrowerHost,settler);
    }

    public Integer getOmniAmount() {
        return omniAmount;
    }

    public Party getBorrowerHost() {
        return borrowerHost;
    }

    public Party getSettler() {
        return settler;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}
