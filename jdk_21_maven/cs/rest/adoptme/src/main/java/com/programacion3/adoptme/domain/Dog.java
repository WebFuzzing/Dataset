package com.programacion3.adoptme.domain;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.*;

@Node("Dog")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Dog {
    @Id
    private String id;
    private String name;
    private String size;       // SMALL, MEDIUM, LARGE
    private Integer weightKg;
    private Integer age;
    private String energy;     // LOW, MEDIUM, HIGH
    private Boolean goodWithKids;
    private Boolean specialNeeds;
    private Integer priority;  // Para priorización de adopción

    // Método personalizado para mantener compatibilidad con código existente
    public Integer getWeight() {
        return this.weightKg;
    }
}
