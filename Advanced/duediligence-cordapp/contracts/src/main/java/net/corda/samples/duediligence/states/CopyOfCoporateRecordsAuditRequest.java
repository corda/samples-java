package net.corda.samples.duediligence.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.samples.duediligence.contracts.CorporateRecordsContract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(CorporateRecordsContract.class)
public class CopyOfCoporateRecordsAuditRequest implements LinearState {


    //private variables
    private Boolean qualification = false;
    private Party originalOwner;
    private Party copyReceiver;
    private UniqueIdentifier originalRequestId;
    private SecureHash originalReportTxId;
    private UniqueIdentifier linearId;
    private Party originalValidater;

    public CopyOfCoporateRecordsAuditRequest(Party originalOwner, Party copyReceiver,
                                             UniqueIdentifier originalRequestId, SecureHash originalReportTxId,
                                             Party originalValidater,Boolean qualification, UniqueIdentifier linearId) {
        this.originalOwner = originalOwner;
        this.copyReceiver = copyReceiver;
        this.originalRequestId = originalRequestId;
        this.originalValidater = originalValidater;
        this.originalReportTxId = originalReportTxId;
        this.linearId = linearId;
        this.qualification = qualification;
    }

    public Party getOriginalOwner() {
        return originalOwner;
    }

    public Party getCopyReceiver() {
        return copyReceiver;
    }

    public UniqueIdentifier getOriginalRequestId() {
        return originalRequestId;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public Party getoriginalValidater() {
        return originalValidater;
    }

    public Boolean getQualification() {
        return qualification;
    }

    public SecureHash getOriginalReportTxId() {
        return originalReportTxId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(copyReceiver);
    }
}
