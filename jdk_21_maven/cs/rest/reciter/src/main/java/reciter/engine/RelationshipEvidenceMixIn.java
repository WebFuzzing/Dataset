package reciter.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;


public abstract class RelationshipEvidenceMixIn {

    // Ignore this field during serialization
    @JsonIgnore
    public abstract double getRelationshipEvidenceTotalScore();
}