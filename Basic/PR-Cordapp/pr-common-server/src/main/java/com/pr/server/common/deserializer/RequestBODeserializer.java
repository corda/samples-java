package com.pr.server.common.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.pr.server.common.bo.impl.DegreeDetailsBO;
import com.pr.server.common.bo.impl.RequestFormBO;

import java.io.IOException;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

public class RequestBODeserializer extends JsonDeserializer<RequestFormBO> {
    @Override
    public RequestFormBO deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        RequestFormBO requestFormBO = new RequestFormBO();

        requestFormBO.setWESReferenceNumber(node.get("wesReferenceNumber") != null ? node.get("wesReferenceNumber").asText() : null);
        requestFormBO.setUniversityName(node.get("universityName") != null ? node.get("universityName").asText() : null);
        requestFormBO.setStudentName(node.get("studentName") != null ? node.get("studentName").asText() : null);
        requestFormBO.setRollNumber(node.get("rollNumber") != null ? node.get("rollNumber").asText() : null);
        requestFormBO.setDegreeName(node.get("degreeName") != null ? node.get("degreeName").asText() : null);
        requestFormBO.setUniversityAddress(node.get("universityAddress") != null ? node.get("universityAddress").asText() : null);
        requestFormBO.setComments(node.get("comments") != null ? node.get("comments").asText() : null);
        requestFormBO.setWesParty(node.get("wesParty") != null ? node.get("wesParty").asText() : null);
        requestFormBO.setUniversityParty(node.get("universityParty") != null ? node.get("universityParty").asText() : null);
        requestFormBO.setConsultantParty(node.get("consultantParty") != null ? node.get("consultantParty").asText() : null);

        return requestFormBO;
    }
}
