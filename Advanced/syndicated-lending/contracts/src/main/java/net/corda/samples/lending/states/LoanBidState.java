package net.corda.samples.lending.states;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.lending.contracts.LoanBidContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(LoanBidContract.class)
public class LoanBidState implements LinearState {

    private StaticPointer<ProjectState> projectDetails;
    private UniqueIdentifier uniqueIdentifier;
    private Party lender;
    private Party borrower;
    private int loanAmount;
    private int tenure;
    private double rateofInterest;
    private int transactionFees;
    private String status;

    public LoanBidState(StaticPointer<ProjectState> projectDetails, UniqueIdentifier uniqueIdentifier,
                        Party lender, Party borrower, int loanAmount, int tenure,
                        double rateofInterest, int transactionFees, String status) {
        this.projectDetails = projectDetails;
        this.uniqueIdentifier = uniqueIdentifier;
        this.lender = lender;
        this.borrower = borrower;
        this.loanAmount = loanAmount;
        this.tenure = tenure;
        this.rateofInterest = rateofInterest;
        this.transactionFees = transactionFees;
        this.status = status;
    }

    public StaticPointer<ProjectState> getProjectDetails() {
        return projectDetails;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public int getLoanAmount() {
        return loanAmount;
    }

    public int getTenure() {
        return tenure;
    }

    public double getRateofInterest() {
        return rateofInterest;
    }

    public int getTransactionFees() {
        return transactionFees;
    }

    public String getStatus() {
        return status;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return uniqueIdentifier;
    }
}
