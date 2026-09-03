package com.example.joinUs.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Node("Member")
public class UserNeo4J {

    @Id
    @Property(name = "member_id")
    private String memberId;

    @Property(name = "member_name")
    private String memberName;

    @Property(name = "city")
    private String cityName;

}

