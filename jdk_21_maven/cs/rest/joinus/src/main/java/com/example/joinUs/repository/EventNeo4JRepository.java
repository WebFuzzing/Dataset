package com.example.joinUs.repository;

import com.example.joinUs.model.neo4j.EventNeo4J;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EventNeo4JRepository extends Neo4jRepository<EventNeo4J, String> {

    @Query("""
            MATCH (u:Event { event_Id: $eventId })
            DETACH DELETE u
            """)
    void deleteEvent(String eventId);

    @Query("MATCH ( e: Event {event_id: $eventId})    " +
            "MATCH ( m: Member {member_id: $userId})   " +
            "MERGE (e)-[:ATTENDS]-> (m) " +
            "MERGE (m)-[:ATTENDS]-> (e) ")
    void addUserAttending(String eventId, String userId);

    @Query("""
            MATCH (e:Event {event_id: $eventId})-[r1:ATTENDS]->(m:Member {member_id: $userId})
            MATCH (m)-[r2:ATTENDS]->(e)
            DELETE r1, r2
            """)
    void revokeUserAttending(String eventId, String userId);

    @Query("""
            MATCH (g:Group {group_id: $groupId})
            MATCH (e:Event {event_id: $eventId})
            MERGE (g)-[:ORGANIZES]->(e)
            MERGE (e)-[:ORGANIZES]->(g)
            """)
    void addGroupEventRelation(String groupId, String eventId);

    @Query("""
            MATCH (g:Group {group_id: $groupId})-[r1:ORGANIZES]->(e:Event {event_id: $eventId})
            MATCH (e)-[r2:ORGANIZES]->(g)
            DELETE r1, r2
            """)
    void removeGroupEventRelation(String groupId, String eventId);

    @Query("""
                MATCH (g:Group {group_id: $groupId})-[:ORGANIZES]-(e:Event)
                RETURN e
            """)
    List<EventNeo4J> findAllEventsOrganizedByGroup(String groupId);

    @Query("""
                MATCH (m:Member {member_id: $memberId})-[:ATTENDS]-(e:Event)
                RETURN e
            """)
    List<EventNeo4J> findAllEventsAttendedByUser(String memberId);

    //(How many users  attended the same events as users of a given event)
    @Query("""
                MATCH (e:Event {event_id: $eventId})
                      <-[:ATTENDS]-(m:Member)
                      -[:ATTENDS]->(otherEvent:Event)
                      <-[:ATTENDS]-(otherMember:Member)
                WHERE otherMember <> m
                RETURN COUNT(DISTINCT otherMember)
            """)
    long countCoAttendees(String eventId);

    /**
     * Recommend events attended by peers (members sharing a group with me)
     */
    @Query("""
                 MATCH (me:Member {member_id: $memberId})-[:MEMBER_OF]->(g:Group)<-[:MEMBER_OF]-(other:Member)-[:ATTENDS]->(e:Event)
                  WHERE e.event_time>datetime() AND NOT (me)-[:ATTENDS]->(e)
                  RETURN DISTINCT e
                  ORDER BY e.event_time
                  LIMIT $limit
            """)
    List<EventNeo4J> recommendEventsByPeerGroupAttendance(
            @Param("memberId") String memberId,
            @Param("limit") long limit
    );//Recommend events that members of the same groups will attend

    @Query("""
                MATCH (me:Member {member_id: $memberId})-[:ATTENDS]->(e1:Event)<-[:ATTENDS]-(other:Member)-[:ATTENDS]->(e2:Event)
                WHERE e2.event_time>datetime() AND NOT (me)-[:ATTENDS]->(e2)
                WITH e2 AS event, COUNT(DISTINCT other) AS score
                ORDER BY score DESC
                LIMIT $limit
                RETURN event
            """)
    List<EventNeo4J> recommendEventsByMembers(String memberId, @Param("limit") long limit);

    @Query("""
                MATCH (m:Member {member_id: $memberId})-[:INTERESTED_IN]->(t:Topic)<-[:HAS_TOPIC]-(g:Group)-[:ORGANIZES]->(e:Event)
                WHERE e.event_time>datetime() AND NOT (m)-[:ATTENDS]->(e)
                WITH e AS event, COUNT(DISTINCT t) AS score
                ORDER BY score DESC
                LIMIT $limit
                RETURN event
            """)
    List<EventNeo4J> recommendEventsBySharedGroupTopics(String memberId, @Param("limit") long limit);

}
