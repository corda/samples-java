package com.pr.server.common.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.pr.server.common.bo.impl.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

public class StudentInfoBODeserializer extends JsonDeserializer<StudentInfoBO> {
    @Override
    public StudentInfoBO deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        StudentInfoBO studentInfoBO = new StudentInfoBO();
        DegreeDetailsBO degreeDetailsBO = new DegreeDetailsBO();
        UniversityBO universityBO = new UniversityBO();
        TranscriptBO transcriptBO = new TranscriptBO();
        List<SemesterBO> semesterBOList = new ArrayList<>();

        studentInfoBO.setCourseDuration(node.get("courseDuration") != null ? node.get("courseDuration").asText() : null);
        studentInfoBO.setDegreeStatus(node.get("degreeStatus") != null ? node.get("degreeStatus").asText() : null);
        if (node.get("degreeDetails") != null) {
            JsonNode degreeDetails = node.get("degreeDetails");
            degreeDetailsBO.setPassingYear(degreeDetails.get("passingYear") != null ? degreeDetails.get("passingYear").asText() : null);
            degreeDetailsBO.setPassingDivision(degreeDetails.get("passingDivision") != null ? degreeDetails.get("passingDivision").asText() : null);
            degreeDetailsBO.setFullName(degreeDetails.get("fullName") != null ? degreeDetails.get("fullName").asText() : null);
            degreeDetailsBO.setFatherName(degreeDetails.get("fatherName") != null ? degreeDetails.get("fatherName").asText() : null);
            degreeDetailsBO.setSpecializationField(degreeDetails.get("specializationField") != null ? degreeDetails.get("specializationField").asText() : null);
            studentInfoBO.setDegreeDetailsBO(degreeDetailsBO);
        }
        if (node.get("universityDetails") != null) {
            JsonNode universityDetails = node.get("universityDetails");
            universityBO.setUniversityType(universityDetails.get("universityType") != null ? universityDetails.get("universityType").asText() : null);
            universityBO.setContactNumber(universityDetails.get("contactNumber") != null ? universityDetails.get("contactNumber").asText() : null);
            studentInfoBO.setUniversityBO(universityBO);
        }
        if (node.get("transcript") != null) {
            JsonNode transcript = node.get("transcript");
            if (transcript.get("semester") != null) {
                JsonNode jsonArrayNode = transcript.get("semester");

                if (jsonArrayNode.isArray()) {
                    for (JsonNode objNode : jsonArrayNode) {
                        SemesterBO semesterBO = new SemesterBO();
                        List<SubjectBO> subjectBOList = new ArrayList<>();
                        semesterBO.setSemesterNumber(objNode.get("semesterNumber") != null ? objNode.get("semesterNumber").asText() : null);
                        semesterBO.setResultDeclaredOnDate(objNode.get("resultDeclaredOnDate") != null ? objNode.get("resultDeclaredOnDate").asText() : null);
                        if (objNode.get("subject") != null) {
                            JsonNode jsonArrayNode1 = objNode.get("subject");
                            if (jsonArrayNode1.isArray()) {
                                for (JsonNode jsonNode : jsonArrayNode1) {
                                    SubjectBO subjectBO = new SubjectBO();
                                    subjectBO.setMarksObtained(jsonNode.get("marksObtained") != null ? jsonNode.get("marksObtained").asDouble() : null);
                                    subjectBO.setSubjectName(jsonNode.get("subjectName") != null ? jsonNode.get("subjectName").asText() : null);
                                    subjectBOList.add(subjectBO);
                                }
                            }
                        }
                        semesterBO.setSubbjectsList(subjectBOList);
                        semesterBOList.add(semesterBO);
                    }
                }

            }
            transcriptBO.setSemester(semesterBOList);
        }
        studentInfoBO.setTranscriptBO(transcriptBO);

        return studentInfoBO;
    }
}
