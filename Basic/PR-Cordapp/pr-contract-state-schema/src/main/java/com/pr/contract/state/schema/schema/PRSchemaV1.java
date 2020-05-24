package com.pr.contract.state.schema.schema;

import com.google.common.collect.ImmutableList;
import com.pr.contract.state.schema.states.PRState;
import com.pr.contract.state.schema.states.PRStatus;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.PersistentStateRef;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * @author Ajinkya Pande & Rishi Kundu
 */

public class PRSchemaV1 extends MappedSchema {

    public PRSchemaV1() {
        super(PRSchema.class, 1, ImmutableList.of(PersistentPR.class));
    }

    @Entity
    @Table(name = "PR")
    public static class PersistentPR extends PersistentState{
        @Column(name = "FirstName")
        private String firstName;

        @Column(name = "LastName")
        private String lastName;

        @Column(name = "CourseName")
        private String courseName;

        @Column(name = "CourseDuration")
        private String courseDuration;

        @Column(name = "University")
        private String university;

        @Column(name = "WesReferenceNumber")
        private String  wesReferenceNumber;

        @Column(name = "Email")
        private String email;

        @Column(name = "PRStatus")
        private PRStatus prStatus;

        @Column(name = "ConsultantParty")
        private AbstractParty consultantParty;

        @Column(name = "WesParty")
        private AbstractParty wesParty;


        @ConstructorForDeserialization
        public PersistentPR(String firstName, String lastName, String courseName, String courseDuration, String university,
                            UniqueIdentifier wesReferenceNumber, String email, PRStatus prStatus, AbstractParty consultantParty,
                            AbstractParty wesParty) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.courseName = courseName;
            this.courseDuration = courseDuration;
            this.university = university;
            this.wesReferenceNumber = wesReferenceNumber.toString();
            this.email = email;
            this.prStatus = prStatus;
            this.consultantParty = consultantParty;
            this.wesParty = wesParty;
        }



        public PersistentPR(PRState prState) {
            this.firstName = prState.getFirstName();
            this.lastName = prState.getLastName();
            this.courseName = prState.getCourseName();
            this.courseDuration = prState.getCourseDuration();
            this.university = prState.getUniversity();
            this.wesReferenceNumber = prState.getWesReferenceNumber().toString();
            this.email = prState.getEmail();
            this.prStatus = prState.getPrStatus();
            this.consultantParty = prState.getConsultantParty();
            this.wesParty = prState.getWesParty();
        }

        public PersistentPR(PersistentStateRef stateRef) {
            super(stateRef);
        }

        public PersistentPR(){

        }

    }

}
