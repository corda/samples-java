package net.corda.samples.multioutput.states;

import net.corda.samples.multioutput.contracts.SubCountContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(SubCountContract.class)
public class SubCountState implements ContractState {
    //private variables
    private Integer amount;
    private Party borrowerMe;
    private Party loaner;

    public SubCountState(Integer amount, Party borrowerMe, Party loaner) {
        this.amount = amount;
        this.borrowerMe = borrowerMe;
        this.loaner = loaner;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return  Arrays.asList(borrowerMe,loaner);
    }

    public Integer getAmount() {
        return amount;
    }

    public Party getBorrowerMe() {
        return borrowerMe;
    }

    public Party getLoaner() {
        return loaner;
    }
}
