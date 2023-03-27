package net.corda.samples.lending.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.lending.contracts.ProjectContract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

// *********
// * ProposalState *
// *********
@BelongsToContract(ProjectContract.class)
public class ProjectState implements LinearState {

    //private variables
    private UniqueIdentifier uniqueIdentifier;
    private String projectDescription;
    private Party borrower;
    private int projectCost;
    private int loanAmount;
    private List<Party> lenders;

    /* Constructor of your Corda state */

    public ProjectState(UniqueIdentifier uniqueIdentifier, String projectDescription, Party borrower,
                        int projectCost, int loanAmount, List<Party> lenders) {
        this.uniqueIdentifier = uniqueIdentifier;
        this.projectDescription = projectDescription;
        this.borrower = borrower;
        this.projectCost = projectCost;
        this.loanAmount = loanAmount;
        this.lenders = lenders;
    }

    // getters

    public String getProjectDescription() {
        return projectDescription;
    }

    public Party getBorrower() {
        return borrower;
    }

    public int getProjectCost() {
        return projectCost;
    }

    public int getLoanAmount() {
        return loanAmount;
    }

    public List<Party> getBidders() {
        return lenders;
    }

    /* This method will indicate who are the participants when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        List<AbstractParty> particiants = new ArrayList<>();
        particiants.add(borrower);
        particiants.addAll(lenders);
        return particiants;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }
}