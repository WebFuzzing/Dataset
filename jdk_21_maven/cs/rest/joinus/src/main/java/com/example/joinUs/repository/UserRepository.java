package com.example.joinUs.repository;

import com.example.joinUs.dto.analytics.*;
import com.example.joinUs.model.mongodb.User;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // Find by exact member name
    @Query("{ '_id': ?0 }")
    List<User> findMemberById(String member_id);

    // Find by exact member name
    @Query("{ 'member_name': ?0 }")
    List<User> findMemberByName(String member_name);
    // Find by member_id (you could also just use findById inherited from MongoRepository)
    //    Optional<User> findByMember_id(String member_id);

    // Find all users in a specific city
    @Query("{ 'city.name': ?0 }")
    List<User> findByCity(String cityName);

    // Find all users who are admins
    @Query("{ 'isAdmin': true }")
    List<User> findAdmins();

    // Find users who have organized a specific group
    @Query("{ 'created_groups': ?0 }")
    List<User> findByCreated_groupsContaining(String groupId);

    // Find users by topic key
    @Query("{ 'topics.topic_name': ?0 }")
    List<User> findByTopicName(String topic_name);

    // Update operations via repository usually use save(entity)
    // Example usage:
    // 1. Retrieve user: Optional<User> u = userRepository.findById(id);
    // 2. Modify fields on User object
    // 3. Save: userRepository.save(user);

    // Delete by member_id
    //    void deleteByMember_id(String member_id);

    // Count users in a specific city
    @Query(value = "{ 'city.name': ?0 }", count = true)
    long countByCity(String cityName);

    @Aggregation(pipeline = {
            "{\n" +
                    "    $project: {\n" +
                    "      userId: '$_id',\n" +
                    "      userName:'$member_name',\n" +
                    "      activityScore: {\n" +
                    "        $add: [\n" +
                    "          { $ifNull: [\"$event_count\", 0] },\n" +
                    "          { $ifNull: [\"$group_count\", 0] }\n" +
                    "        ]\n" +
                    "      },\n" +
                    "      _id:0\n" +
                    "    }\n" +
                    "  }",
            "{ $sort: { activityScore: -1 } }",
            "{ $limit: ?0 }"
    })
    List<ActivityScorePerUserAnalytic> topUsersByActivityScore(int limit);

    //Topic Popularity Among Users
    @Aggregation(pipeline = {
            "{ $unwind: '$topics' }",
            "{ $group: { _id: '$topics.topic_name', usersCount: { $sum: 1 } } }",
            "{ $sort: { usersCount: -1 } }",
            "{ $limit: ?0 }",
            " {$project: {topic:'$_id',usersCount:1,_id:0}}"
    })
    List<TopicPerUserAnalytic> mostPopularUserTopics(int limit);

    //Topic popularity per city (top topics inside each city) showing shows for each city, which topics appear most.
    @Aggregation(pipeline = {
            "{ $unwind: '$topics' }",
            "{ $group: { _id: { city: '$city.name', topic: '$topics.topic_name' }, usersCount: { $sum: 1 } } }",
            "{ $sort: { usersCount: -1 } }",
            "{ $group: { _id: '$_id.city', topTopics: { $push: { topic: '$_id.topic', usersCount: '$usersCount' } } } }",
            "{ $project: { city: '$_id', topTopics: { $slice: ['$topTopics', ?0] }, _id: 0 } }",
    })
    List<TrendingTopicPerCityAnalytic> topTrendingTopicsPerCity(int topicCount);

    @Aggregation(pipeline = {
            "{ $project: { city: '$city.name', isActive: { $gt: [ { $size: { $ifNull: ['$upcoming_events', []] } }, 0 ] } } }",
            "{ $group: { _id: '$city', totalMembers: { $sum: 1 }, activeMembers: { $sum: { $cond: ['$isActive', 1, 0] } } } }",
            "{ $project: { city: '$_id', totalMembers: 1, activeMembers: 1, activityRatio: { $divide: ['$activeMembers', '$totalMembers'] }, _id: 0 } }",
            "{ $sort: { activityRatio: -1 } }",
            "{ $limit: 10 }"
    })
    List<CityActivityAnalytic> mostActiveCities();

    @Aggregation(pipeline = {
            "{ $group: { _id: '$city.name', usersCount: { $sum: 1 } } }",
            "{ $sort: { usersCount: -1 } }",
            "{ $limit: ?0 }",
            "{$project:{city:'$_id',usersCount:1,_id:0}"
    })
    List<UsersPerCityAnalytic> countUsersByCity(int limit);

}
