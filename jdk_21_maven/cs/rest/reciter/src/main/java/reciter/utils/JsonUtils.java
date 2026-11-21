package reciter.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.engine.RelationshipEvidenceMixIn;
import reciter.engine.analysis.evidence.RelationshipEvidence;

public class JsonUtils {

	public static ObjectMapper configureObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Register the mix-in to the RelationshipEvidence class
        objectMapper.addMixIn(RelationshipEvidence.class, RelationshipEvidenceMixIn.class);

        return objectMapper;
    }
}

