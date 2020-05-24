package com.pr.server.common.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.pr.server.common.bo.impl.PRBO;

import java.io.IOException;


/**
 * @author Ajinkya Pande & Rishi Kundu
 */

public class PRBODeserializer extends JsonDeserializer<PRBO> {


    @Override
    public PRBO deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);


        return new PRBO(node.get("firstName") != null ? node.get("firstName").asText() : null,
                node.get("lastName") != null ? node.get("lastName").asText() : null,
                node.get("courseName") != null ? node.get("courseName").asText() : null,
                node.get("courseDuration") != null ? node.get("courseDuration").asText() : null,
                node.get("university") != null ? node.get("university").asText() : null,
                node.get("email") != null ? node.get("email").asText() : null,
                node.get("prStatus") != null ? node.get("prStatus").asText() : null,
                node.get("consultantParty") != null ? node.get("consultantParty").asText() : "O=Consultants,L=London,C=GB",
                node.get("wesParty") != null ? node.get("wesParty").asText() : "O=Wes,L=London,C=GB",
                node.get("amount") != null ? node.get("amount").asInt() : null);

    }
}


