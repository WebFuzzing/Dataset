package com.example.joinUs.repository;

import com.example.joinUs.dto.GroupCommunityDTO;
import com.example.joinUs.model.neo4j.GroupNeo4J;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Repository
public class GroupCommunityRepository {

    @Autowired
    private final Neo4jClient neo4jClient;

    public List<GroupCommunityDTO> findGroupCommunities(long minShared, long limit) {

        String cypher = """
                  MATCH (g1:Group)<-[:MEMBER_OF]-(m:Member)-[:MEMBER_OF]->(g2:Group)
                  WHERE g1 <> g2 AND g1.group_id < g2.group_id
                  WITH g1, g2, count(DISTINCT m) AS sharedMembers
                  WHERE sharedMembers >= $minShared
                  RETURN g1 AS group1, g2 AS group2, sharedMembers AS sharedMembers
                  ORDER BY sharedMembers DESC
                  LIMIT $limit
                """;

        return neo4jClient.query(cypher)
                .bindAll(Map.of("minShared", minShared, "limit", limit))
                .fetchAs(GroupCommunityDTO.class)
                .mappedBy((typeSystem, record) -> {
                    var n1 = record.get("group1").asNode();
                    var n2 = record.get("group2").asNode();

                    GroupNeo4J g1 = GroupNeo4J.builder()
                            .groupId(n1.get("group_id").asString())
                            .groupName(n1.get("group_name").asString(null))
                            .description(n1.get("description").asString(null))
                            .cityName(n1.get("city").asString(null))
                            .categoryName(n1.get("category_name").asString(null))
                            .build();

                    GroupNeo4J g2 = GroupNeo4J.builder()
                            .groupId(n2.get("group_id").asString())
                            .groupName(n2.get("group_name").asString(null))
                            .description(n2.get("description").asString(null))
                            .cityName(n2.get("city").asString(null))
                            .categoryName(n2.get("category_name").asString(null))
                            .build();

                    return GroupCommunityDTO.builder()
                            .group1(g1)
                            .group2(g2)
                            .sharedMembers(record.get("sharedMembers").asLong())
                            .build();
                })
                .all().stream().toList();
    }
}