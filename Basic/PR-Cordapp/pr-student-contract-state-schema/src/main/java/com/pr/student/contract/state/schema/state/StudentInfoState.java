package com.pr.student.contract.state.schema.state;

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

import java.util.List;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

@CordaSerializable
public class StudentInfoState {

    private String rollNumber;
    private String courseDuration;
    private String degreeStatus;
    private DegreeDetails degreeDetails;
    private Transcript transcript;
    private University university;

    public String getRollNumber() {
        return rollNumber;
    }

    public String getCourseDuration() {
        return courseDuration;
    }

    public String getDegreeStatus() {
        return degreeStatus;
    }

    public DegreeDetails getDegreeDetails() {
        return degreeDetails;
    }

    public Transcript getTranscript() {
        return transcript;
    }

    public University getUniversity() {
        return university;
    }

    @ConstructorForDeserialization
    public StudentInfoState(String rollNumber,
                            String courseDuration, String degreeStatus, DegreeDetails degreeDetails,
                            Transcript transcript, University university) {
        this.rollNumber = rollNumber;
        this.courseDuration = courseDuration;
        this.degreeStatus = degreeStatus;
        this.degreeDetails = degreeDetails;
        this.transcript = transcript;
        this.university = university;
    }
}
