package com.pr.student.contract.state.schema.schema;

import com.google.common.collect.ImmutableList;
import com.pr.student.contract.state.schema.state.RequestForm;
import com.pr.student.contract.state.schema.state.StudentInfoState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.PersistentStateRef;
import net.corda.core.serialization.ConstructorForDeserialization;

import javax.persistence.*;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

public class RequestFormSchemaV1 extends MappedSchema {

    public RequestFormSchemaV1() {
        super(RequestFormSchema.class, 1, ImmutableList.of(PersistentRequestForm.class));
    }

    @Entity
    @Table(name = "academic_wes_request_form")
    public static class PersistentRequestForm extends PersistentState {

        @Column(name = "studentName")
        private String studentName;
        @Column(name = "rollNumber")
        private String rollNumber;
        @Column(name = "wes_reference_number")
        private String wes_reference_number;


        @ConstructorForDeserialization
        public PersistentRequestForm(String studentName, String rollNumber, String wes_reference_number) {
            this.studentName = studentName;
            this.rollNumber = rollNumber;
            this.wes_reference_number = wes_reference_number;
        }

        public PersistentRequestForm(RequestForm other) {
            this.studentName = other.getStudentName();
            this.rollNumber = other.getRollNumber();
            this.wes_reference_number = other.getWesReferenceNumber();
        }


        public PersistentRequestForm(PersistentStateRef stateRef) {
            super(stateRef);
        }

        public PersistentRequestForm() {
        }
    }
}
