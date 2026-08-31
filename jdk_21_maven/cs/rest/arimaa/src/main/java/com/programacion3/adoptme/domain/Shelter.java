package com.programacion3.adoptme.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.*;

@Node("Shelter")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Shelter {
    @Id
    private String id;
    private String name;
    private Integer capacity;
}
