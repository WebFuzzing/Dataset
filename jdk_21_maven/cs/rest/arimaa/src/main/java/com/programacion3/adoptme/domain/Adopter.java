package com.programacion3.adoptme.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.*;

@Node("Adopter")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Adopter {
    @Id
    private String id;
    private String name;
    private Integer budget;
    private Boolean hasYard;
    private Boolean hasKids;
    private Integer maxDogs;
}
