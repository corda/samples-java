package net.corda.samples.duediligence.states;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.duediligence.contracts.DueDChecklistContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(DueDChecklistContract.class)
public class DueDChecklist implements LinearState {

    //private variables
    private int numberOfapprovalsNeeded;
    private String status = "INCOMPLETE";
    private Party operationNode;
    private Party reportTo;
    private List<UniqueIdentifier> attachedApprovals = new ArrayList<UniqueIdentifier>();
    private UniqueIdentifier linearID;

    /* Constructor of your Corda state */
    @ConstructorForDeserialization
    public DueDChecklist(int numberOfapprovalsNeeded, String status, Party operationNode, Party reportTo, List<UniqueIdentifier> attachedApprovals, UniqueIdentifier linearID) {
        this.numberOfapprovalsNeeded = numberOfapprovalsNeeded;
        this.status = status;
        this.operationNode = operationNode;
        this.reportTo = reportTo;
        this.attachedApprovals = attachedApprovals;
        this.linearID = linearID;
    }

    //Modification will
    public DueDChecklist(int numberOfapprovalsNeeded, Party operationNode, Party reportTo, UniqueIdentifier linearID) {
        this.numberOfapprovalsNeeded = numberOfapprovalsNeeded;
        this.operationNode = operationNode;
        this.reportTo = reportTo;
        this.linearID = linearID;
    }

    //getters
    public int getNumberOfapprovals() { return numberOfapprovalsNeeded; }
    public String getStatus() { return status; }
    public Party getOperationNode() { return operationNode; }

    public int getNumberOfapprovalsNeeded() {
        return numberOfapprovalsNeeded;
    }

    public List<UniqueIdentifier> getAttachedApprovals() {
        return attachedApprovals;
    }

    public void uploadApproval(UniqueIdentifier approvalId){
        List<UniqueIdentifier> copyOfExistingList = new ArrayList<UniqueIdentifier>();
        for (UniqueIdentifier id : attachedApprovals){
            copyOfExistingList.add(id);
        }
        copyOfExistingList.add(approvalId);
        this.attachedApprovals = copyOfExistingList;
    }


    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(operationNode,reportTo);
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearID;
    }
}