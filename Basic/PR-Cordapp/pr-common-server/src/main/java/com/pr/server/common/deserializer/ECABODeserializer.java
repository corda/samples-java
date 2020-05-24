package com.pr.server.common.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.pr.server.common.bo.impl.ECAStateBO;
import com.pr.server.common.bo.impl.PRBO;
import com.pr.server.common.bo.impl.RequestFormBO;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @author Rishi Kundu and Ajinkya Pande
 */

public class ECABODeserializer extends JsonDeserializer<ECAStateBO> {

    @Override
    public ECAStateBO deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        ECAStateBO ecaStateBO = new ECAStateBO();

        ecaStateBO.setNameOnCredential(node.get("nameOnCredential") != null ? node.get("nameOnCredential").asText() : null);
        ecaStateBO.setCredentialAuth(node.get("credentialAuth") != null ? node.get("credentialAuth").asText() : null);
        ecaStateBO.setCountry(node.get("country") != null ? node.get("country").asText() : null);
        ecaStateBO.setCredential(node.get("credential") != null ? node.get("credential").asText() : null);
        ecaStateBO.setYear(node.get("year") != null ? node.get("year").asText() : null);
        ecaStateBO.setAwardedBy(node.get("awardedBy") != null ? node.get("awardedBy").asText() : null);
        ecaStateBO.setStatus(node.get("status") != null ? node.get("status").asText() : null);
        ecaStateBO.setMajor(node.get("major") != null ? node.get("major").asText() : null);
        ecaStateBO.setEquivalency(node.get("equivalency") != null ? node.get("equivalency").asText() : null);

        return ecaStateBO;

    }
}
