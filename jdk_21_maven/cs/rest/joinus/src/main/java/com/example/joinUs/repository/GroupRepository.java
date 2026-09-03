package com.example.joinUs.repository;

import com.example.joinUs.dto.analytics.GroupsPerCityAnalytic;
import com.example.joinUs.dto.analytics.GroupsPerOrganizerAnalytic;
import com.example.joinUs.dto.analytics.GroupsPerTopicAnalytic;
import com.example.joinUs.model.mongodb.Group;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface GroupRepository extends MongoRepository<Group, String> {

    String fields = "{'_id':1,'group_name':1,'upcoming_events':1,'city.name':1','category.name':1,'member_count':1,'event_count':1}";
    String projection = "{$project:" + fields + "}";

    Optional<Group> findById(String id);

    Page<Group> findAll(Pageable pageable);

    boolean existsById(String id);

    void deleteById(String id);

    @Query("{'organizers.member_id' : ?0 }")
    List<Group> findGroupsByOrganizerId(String id);

    //{$or:[{group_id:"5817263"},{group_name:"San Francisco Startup Socials"}]}

    @Query("{$or:[{ '_id': ?0 },{ 'group_name': ?1 }]}")
    List<Group> findGroupByGroupIdOrGroupName(String groupId, String groupName);

    // Aggregations for Admin page
    //Top Cities by Groups Created in the Past 10 Years

    // Count Groups per Category
    @Aggregation(pipeline = {
            "{ $unwind: 'topics' }",
            "{ $group: { _id: '$topics.topic_name', groupsCount: { $sum: 1 } } }",
            "{ $sort: { groupsCount: -1 } }",
            "{ $limit: ?0 }",
            "{$project: {topic:'$_id',groupsCount:1,_id:0}}"
    })
    List<GroupsPerTopicAnalytic> countGroupsByTopic(int limit);

    //Top Cities by Groups Created in the Past 10 Years
    @Aggregation(pipeline = {
            "{ $match: { created: { $gte: { $dateSubtract: { startDate: '$$NOW', unit: 'year', amount: 10 } } } } }",
            "{ $group: { _id: '$city.name', groupsCreated: { $sum: 1 } } }",
            "{ $sort: { groupsCreated: -1 } }",
            "{ $limit: ?0 }",
            "{ $project: { city: '$_id', groupsCreated: 1, _id: 0 } }"
    })
    List<GroupsPerCityAnalytic> topCitiesByGroupsLast10Years(int limit);

    @Aggregation(pipeline = {
            "{ $match: { created: { $gte: { $dateSubtract: { startDate: '$$NOW', unit: 'month', amount: 1 } } } } }",
            "{ $unwind: '$attendees' }",
            "{ $group: { _id: '$category_name', attendance: { $sum: 1 } } }",
            "{ $sort: { attendance: -1 } }",
            "{ $limit: 10 }",
            "{ $project: { category: '$_id', attendance: 1, _id: 0 } }"
    })
    List<Document> trendingCategoriesLastMonth();

    //Groups created per year (trend) showing Growth of groups over time.
    @Aggregation(pipeline = {
            "{ $match: { created: { $ne: null } } }",
            "{ $group: { _id: { $year: '$created' }, groupsCreated: { $sum: 1 } } }",
            "{ $sort: { _id: 1 } }",
            "{ $project: { year: '$_id', groupsCreated: 1, _id: 0 } }"
    })
    List<Document> groupsCreatedPerYear();

    //Organizer leaderboard (top organizers by number of groups)
    @Aggregation(pipeline = {
            "{ $unwind: '$organizer_members' }",
            "{ $group: { _id: '$organizer_members.member_id', organizerName: { $first: '$organizer_members.member_name' }" +
                    ", groupsOrganized: { $sum: 1 } } }",
            "{ $sort: { groupsOrganized: -1 } }",
            "{ $limit: ?0 }",
            "{ $project: { userId: '$_id', organizerName: 1, groupsOrganized: 1, _id: 0 } }"
    })
    List<GroupsPerOrganizerAnalytic> topOrganizersByGroups(int limit);
}
