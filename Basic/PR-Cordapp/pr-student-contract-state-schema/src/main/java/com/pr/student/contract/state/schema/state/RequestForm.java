package com.pr.student.contract.state.schema.state;

import com.pr.student.contract.state.schema.contract.RequestFormContract;
import com.pr.student.contract.state.schema.schema.RequestFormSchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@CordaSerializable
@BelongsToContract(RequestFormContract.class)
public class RequestForm implements LinearState, QueryableState {
    private String wesReferenceNumber;
    private String universityName;
    private String studentName;
    private UniqueIdentifier requestId;
    private String rollNumber;
    private String degreeName;
    private String universityAddress;
    private StudentInfoState studentInfoState;
    private AbstractParty wesParty;
    private AbstractParty consultantParty;
    private AbstractParty universityParty;
    private RequestStatus requestStatus;
    private String stateHash;

    public String getWesReferenceNumber() {
        return wesReferenceNumber;
    }

    public UniqueIdentifier getRequestId() {
        return requestId;
    }

    public String getStateHash() {
        return stateHash;
    }

    public void setStateHash(String stateHash) {
        this.stateHash = stateHash;
    }

    public AbstractParty getWesParty() {
        return wesParty;
    }

    public AbstractParty getConsultantParty() {
        return consultantParty;
    }

    public AbstractParty getUniversityParty() {
        return universityParty;
    }

    public StudentInfoState getStudentInfoState() {
        return studentInfoState;
    }

    public String getUniversityName() {
        return universityName;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public String getUniversityAddress() {
        return universityAddress;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    @ConstructorForDeserialization
    public RequestForm(UniqueIdentifier requestId, String wesReferenceNumber, String universityName, String studentName,
                       String rollNumber, String degreeName, String universityAddress, StudentInfoState studentInfoState,
                       AbstractParty wesParty, AbstractParty consultantParty, AbstractParty universityParty,
                       RequestStatus requestStatus, String stateHash) {
        this.wesReferenceNumber = wesReferenceNumber;
        this.universityName = universityName;
        this.studentName = studentName;
        this.requestId = requestId;
        this.rollNumber = rollNumber;
        this.degreeName = degreeName;
        this.universityAddress = universityAddress;
        this.studentInfoState = studentInfoState;
        this.wesParty = wesParty;
        this.consultantParty = consultantParty;
        this.universityParty = universityParty;
        this.requestStatus = requestStatus;
        this.stateHash = stateHash;
    }

    public RequestForm(UniqueIdentifier requestId, String WESReferenceNumber, String universityName,
                       String studentName, String degreeName,
                       String universityAddress, AbstractParty wesParty, AbstractParty consultantParty,
                       AbstractParty universityParty, RequestStatus requestStatus, String rollNumber) {
        this.wesReferenceNumber = WESReferenceNumber;
        this.universityName = universityName;
        this.studentName = studentName;
        this.requestId = requestId;
        this.rollNumber = rollNumber;
        this.degreeName = degreeName;
        this.universityAddress = universityAddress;
        this.wesParty = wesParty;
        this.consultantParty = consultantParty;
        this.universityParty = universityParty;
        this.requestStatus = requestStatus;
    }

    public RequestForm(RequestForm other) {
        this.wesReferenceNumber = other.wesReferenceNumber;
        this.universityName = other.universityName;
        this.studentName = other.studentName;
        this.rollNumber = other.rollNumber;
        this.requestId = other.requestId;
        this.degreeName = other.degreeName;
        this.universityAddress = other.universityAddress;
        this.studentInfoState = other.studentInfoState;
        this.wesParty = other.wesParty;
        this.consultantParty = other.consultantParty;
        this.universityParty = other.universityParty;
        this.requestStatus = other.requestStatus;
    }

    public RequestForm(RequestForm other, RequestStatus requestStatus) {
        this.wesReferenceNumber = other.wesReferenceNumber;
        this.universityName = other.universityName;
        this.studentName = other.studentName;
        this.rollNumber = other.rollNumber;
        this.requestId = other.requestId;
        this.degreeName = other.degreeName;
        this.universityAddress = other.universityAddress;
        this.studentInfoState = other.studentInfoState;
        this.wesParty = other.wesParty;
        this.consultantParty = other.consultantParty;
        this.universityParty = other.universityParty;
        this.requestStatus = requestStatus;
    }

    public RequestForm(RequestForm other, StudentInfoState studentInfoState, RequestStatus requestStatus) {
        this.wesReferenceNumber = other.wesReferenceNumber;
        this.universityName = other.universityName;
        this.studentName = other.studentName;
        this.rollNumber = other.rollNumber;
        this.requestId = other.requestId;
        this.degreeName = other.degreeName;
        this.universityAddress = other.universityAddress;
        this.wesParty = other.wesParty;
        this.consultantParty = other.consultantParty;
        this.universityParty = other.universityParty;
        this.requestStatus = requestStatus;
        this.studentInfoState = studentInfoState;
    }


    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return requestId;
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return Arrays.asList(new RequestFormSchemaV1());
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof RequestFormSchemaV1) {
            return new RequestFormSchemaV1.PersistentRequestForm(this);
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(wesParty, consultantParty, universityParty);
    }
}
