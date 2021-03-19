package net.corda.samples.duediligence.states;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.samples.duediligence.contracts.CorporateRecordsContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(CorporateRecordsContract.class)
public class CorporateRecordsAuditRequest implements LinearState {
    /*This can include reviewing incorporation documents, company constitutions,
    organisational charts, a list of security holders, employee share plans and any
    options granted to acquire securities.*/

    //private variables
    private Boolean qualification = false;
    private Party applicant;
    private Party validater;
    private int numberOfFiles;
    private UniqueIdentifier linearId;

    /* Constructor of your Corda state */
    @ConstructorForDeserialization
    public CorporateRecordsAuditRequest(Party applicant, Party validater, int numberOfFiles, UniqueIdentifier linearId, Boolean qualification) {
        this.applicant = applicant;
        this.validater = validater;
        this.numberOfFiles = numberOfFiles;
        this.linearId = linearId;
        this.qualification = qualification;
    }

    public CorporateRecordsAuditRequest(Party applicant, Party validater, int numberOfFiles, UniqueIdentifier linearId) {
        this.applicant = applicant;
        this.validater = validater;
        this.numberOfFiles = numberOfFiles;
        this.linearId = linearId;
    }

    public CorporateRecordsAuditRequest(Party applicant, Party validater, int numberOfFiles) {
        this.qualification = qualification;
        this.applicant = applicant;
        this.validater = validater;
        this.numberOfFiles = numberOfFiles;
        this.linearId = new UniqueIdentifier();
    }

    //getters
    public Boolean getQualification() {        return qualification;    }
    public Party getApplicant() {        return applicant;    }
    public Party getValidater() {        return validater;    }
    public int getNumberOfFiles() {        return numberOfFiles;    }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(applicant,validater);
    }

    public void validatedAndApproved (){
        this.qualification = true;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return this.linearId;
    }
}