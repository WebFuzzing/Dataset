package com.example.joinUs.repository;

import com.example.joinUs.model.neo4j.GroupNeo4J;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface GroupNeo4JRepository extends Neo4jRepository<GroupNeo4J, String> {

    @Query("""
            MATCH (u:Group { group_id: $groupId })
            DETACH DELETE u
            """)
    void deleteGroup(String groupId);

    @Query("""
                MATCH (m:Member {member_id: $memberId})-[:MEMBER_OF]->(g:Group)
                RETURN DISTINCT g
            """)
    List<GroupNeo4J> findAllGroupsOfUser(String memberId);

    @Query("""
            MATCH (g:Group {group_id: $groupId})<-[:MEMBER_OF]-(m:Member),
                  (g)-[:ORGANIZES]->(e:Event)<-[:ATTENDS]-(m)
            RETURN COUNT(DISTINCT m)
            """)
    long countActiveMembersInGroup(String groupId);

    @Query("""
                MATCH (m:Member {member_id: $memberId})-[:INTERESTED_IN]->(t:Topic)<-[:HAS_TOPIC]-(g:Group)
                WHERE NOT (m)-[:MEMBER_OF]->(g)
                WITH g AS group, COUNT(DISTINCT t) AS score
                ORDER BY score DESC
                LIMIT 10
                RETURN group
            """)
    List<GroupNeo4J> recommendGroupsByTopics(String memberId, @Param("limit") long limit);
    //Recommend groups to a member based on the topics they are interested in

    @Query("""
                MATCH (me:Member {member_id: $memberId})-[:MEMBER_OF]->(g1:Group)<-[:MEMBER_OF]-(other:Member)-[:MEMBER_OF]->(g2:Group)
                WHERE NOT (me)-[:MEMBER_OF]->(g2)
                WITH g2 AS group, COUNT(DISTINCT other) AS score
                ORDER BY score DESC
                LIMIT $limit
                RETURN group
            """)
    List<GroupNeo4J> recommendGroupsBySimilarMembers(String memberId, @Param("limit") long limit);
    //Recommend groups for a member based on their current groups and other groups of the other members

}
