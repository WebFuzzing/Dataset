package com.example.joinUs.repository;

import com.example.joinUs.model.neo4j.TopicNeo4J;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TopicNeo4JRepository extends Neo4jRepository<TopicNeo4J, String> {

    // List<Topic_Neo4J> findByTopicName(String topicName);

    @Query("""
            MATCH (u:Topic { topic_id: $topicId })
            DETACH DELETE u
            """)
    void deleteTopic(String topicId);

    @Query("""
                MATCH  (m:Member)-[:INTERESTED_IN]->(t)-[:HAS_TOPIC]->(g:Group)
                WITH t, count(DISTINCT m) AS memberCount, count(DISTINCT g) AS groupCount 
                 ORDER BY (memberCount+groupCount)
                RETURN t
                LIMIT $limit
            """)
    List<TopicNeo4J> findMostPopularTopicsAmongGroupsAndUsers(@Param("limit") long limit);
    //Find topics of groups, which users are most interested in

    @Query("""
                MATCH  (m:Member)-[:INTERESTED_IN]->(t:Topic)-[:HAS_TOPIC]->(g:Group {group_id: $groupId })
                WITH t, count(DISTINCT m) AS memberCount, count(DISTINCT g) AS groupCount
                 ORDER BY (memberCount+groupCount)
                RETURN t
                LIMIT $limit
            """)
    List<TopicNeo4J> findGroupsTopicsWhichUsersAreMostInterestedIn(@Param("limit") long limit,
            @Param("groupId") String groupId);

}