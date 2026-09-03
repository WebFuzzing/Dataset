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
@Node("Topic")
public class TopicNeo4J {

    @Id
    @Property(name = "topic_id")
    private String topicId;

    @Property(name = "topic_name")
    private String topicName;

}

