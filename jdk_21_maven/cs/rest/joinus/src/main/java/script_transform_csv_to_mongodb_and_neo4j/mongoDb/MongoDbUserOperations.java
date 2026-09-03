package script_transform_csv_to_mongodb_and_neo4j.mongoDb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.util.*;
import java.util.concurrent.Future;


public class MongoDbUserOperations {

    public MongoClient mongoClient;
    public MongoDatabase mongoOriginalDatabase;
    public static String newUserCollectionName = "members";
    public static List<String> metaMembersIds;
    private final ParallelExecutor parallelExecutor;

    public MongoCollection getNewUserCollection() {
        mongoOriginalDatabase.createCollection(newUserCollectionName);
        return mongoOriginalDatabase.getCollection(newUserCollectionName);
    }

    public MongoDbUserOperations(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoDbUserOperations(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            MongoCollection userCollection, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoDbUserOperations(MongoClient mongoClient, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        mongoOriginalDatabase = mongoClient.getDatabase("joinUs");
        this.parallelExecutor = parallelExecutor;
    }

    /**
     * As we agreed we would embed topics to a member in MongoDB for aggregations purpose although we also have them in
     * the GraphDB
     *
     * @param memberId
     * @return
     */
    public List<Document> extractTopicsPerMember(String memberId) {

        MongoCollection topicCollection = MongoDataLoader.csvDocuments.getCollection("members_topics.csv");

        List<Document> userTopics = new ArrayList<>();
        try (MongoCursor<Document> cursor = topicCollection
                .find(new Document("member_id", memberId))
                //                .projection(new Document("topic_id", 1).append("topic_name", 1).append("_id",0))
                .cursor()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                Document documentToAdd = new Document();
                documentToAdd.append("topic_id", document.getString("topic_id"));
                documentToAdd.append("topic_name", document.getString("topic_name"));
                userTopics.add(documentToAdd);
            }
        }

        return userTopics;
    }

    public HashMap<String, Double> extractGroupCountPerMember() {

        HashMap<String, Double> groupCount = new HashMap<>();

        //        //rsvps.csv
        //        MongoCollection rsvpsCollection = CsvToMongoTransformer.csvDocuments.getCollection("rsvps.csv");
        //
        //        List<Document> aggregationList = Arrays.asList(new Document("$group",
        //                        new Document("_id", "$member_id")
        //                                .append("groupCount",
        //                                        new Document("$sum",
        //                                                new Document("$cond", Arrays.asList(new Document("$ifNull", Arrays.asList("$group_id", false)), 1L, 0L))))),
        //                new Document("$project",
        //                        new Document("member_id", "$_id")
        //                                .append("groupCount",
        //                                        new Document("$toDouble", "$groupCount"))
        //                                .append("_id", 0L)));
        //        int groupCountFromRsvps = 0;
        //        long groupCountFromMembers=0;
        //        try (MongoCursor<Document> mongoCursor = rsvpsCollection.aggregate(aggregationList).cursor()) {
        //
        //            if (mongoCursor.hasNext()) {
        //                Document document = mongoCursor.next();
        //                groupCount.put(document.getString("member_id"), document.getDouble("groupCount"));
        //            }
        //        }

        List<Document> membersAggregationList = Arrays.asList(new Document("$group",
                        new Document("_id", "$member_id")
                                .append("groupCount",
                                        new Document("$addToSet", "$group_id"))),
                new Document("$project",
                        new Document("member_id", "$_id")
                                .append("groupCount",
                                        new Document("$size", "$groupCount"))),
                new Document("$project",
                        new Document("member_id", 1L)
                                .append("groupCount",
                                        new Document("$toDouble", "$groupCount"))));

        MongoCollection membersCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
        MongoCursor<Document> cursor = membersCollection.aggregate(membersAggregationList).cursor();

        while (cursor.hasNext()) {
            Document document = cursor.next();
            String member_id = document.getString("member_id");
            groupCount.put(member_id, document.getDouble("groupCount"));
        }

        return groupCount;
    }

    public static HashMap<String, Double> extractEventCountPerMember() {
        //rsvps.csv
        HashMap<String, Double> eventCount = new HashMap<>();
        MongoCollection rsvpsCollection = MongoDataLoader.csvDocuments.getCollection("rsvps.csv");

        List<Document> aggregationList = Arrays.asList(
                new Document("$group",
                        new Document("_id", "$member_id")
                                .append("count",
                                        new Document("$sum", 1))),
                new Document("$project",
                        new Document("member_id", "$_id")
                                .append("event_count",
                                        new Document("$toDouble", "$count"))
                ));

        try (MongoCursor<Document> mongoCursor = rsvpsCollection.aggregate(aggregationList).cursor()) {

            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                eventCount.put(document.getString("member_id"), document.getDouble("event_count"));
            }
        }

        return eventCount;
    }

    public List<String> extractGroupIdsWhereOrganizer(String memberName) {
        List<String> list = new ArrayList<>();
        MongoCollection groupCollection = MongoDataLoader.csvDocuments.getCollection("groups.csv");

        MongoCursor<Document> mongoCursor = groupCollection.find(Filters.eq("organizer_name", memberName))
                .projection(new Document("group_id", 1))
                .cursor();
        while (mongoCursor.hasNext()) {
            list.add(mongoCursor.next().getString("group_id"));
        }
        return list;
    }

    /**
     * Should call this after modifying the events  , so that the event_time makes sense, and thus we avoid returning
     * empty lists
     *
     * @param memberId
     * @return
     */
    public List<Document> extractFutureEventsPerMember(String memberId) {
        //rsvps.csv
        MongoCollection rsvpsCollection = MongoDataLoader.csvDocuments.getCollection("rsvps.csv");
        List<String> eventIdsFromRsvps = new ArrayList<>();
        List<Document> upcomingEvents;
        try (MongoCursor<Document> cursor = rsvpsCollection.find(Filters.eq("member_id", memberId)).cursor()) {
            upcomingEvents = new ArrayList<>();
            if (cursor != null) {
                while (cursor.hasNext()) {
                    String event_id = (cursor.next()).getString("event_id");
                    eventIdsFromRsvps.add(event_id);
                }

                MongoCollection eventCollection = MongoDataLoader.newMongoDatabase.getCollection("events");

                MongoCursor eventCursor = eventCollection.find
                        (
                                Filters.and(
                                        Filters.in("_id", eventIdsFromRsvps)
                                        , Filters.gt("event_time", new Date()
                                        )
                                )
                        ).cursor();

                if (eventCursor != null) {
                    while (eventCursor.hasNext()) {
                        Document documentToEmbed = new Document();
                        Document eventDocument = (Document) eventCursor.next();
                        Date event_time = eventDocument.getDate("event_time");

                        //                    if (event_time.after(new Date())) {

                        documentToEmbed.append("event_id", eventDocument.getString("_id"));
                        documentToEmbed.append("event_name", eventDocument.getString("event_name"));
                        documentToEmbed.append("event_time", event_time);

                        upcomingEvents.add(documentToEmbed);
                        //                    }
                    }
                }
            }
        }
        return upcomingEvents;
    }

    public static Document extractCityDocumentToEmbedPerMember(Document oldDocument) {

        if (oldDocument == null) return new Document();
        String cityName = oldDocument.getString("city");
        return MongoDbCityOperation.extractCityToEmbedFromCityName(cityName);

    }

    public static Document extractMemberDocument(Document oldDocument) {

        Document document = new Document();
        if (oldDocument == null) return document;
        document.append("_id", oldDocument.getString("member_id"));
        document.append("member_name", oldDocument.getString("member_name"));
        MongoDataLoader.assignIfFound(document, "hometown", oldDocument.getString("hometown"));
        MongoDataLoader.assignIfFound(document, "bio", oldDocument.getString("bio"));

        return document;
    }

    public void createMemberCollection(boolean includeOnlyMembersWithModifiedId) throws Exception {

        List<String> memberIds = metaMembersIds != null ? metaMembersIds : retrieveIdsFromMetaMembers();
        boolean includeFilter = includeOnlyMembersWithModifiedId && !memberIds.isEmpty();

        MongoCollection oldMemberCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
        MongoCollection newMemberCollection = getNewUserCollection();

        Document finalDocument;

        Future[] futures = new Future[5];

        try (MongoCursor<Document> mongoCursor = oldMemberCollection.find().cursor()) {

            futures[0] = parallelExecutor.submit(() -> extractEventCountPerMember());
            futures[1] = parallelExecutor.submit(() -> extractGroupCountPerMember());

            HashMap<String, Double> groupCount = (HashMap<String, Double>) futures[1].get();
            HashMap<String, Double> eventCount = (HashMap<String, Double>) futures[0].get();

            List<Document> membersWithId = new ArrayList<>();
            Set<String> membersAlreadyEvaluated = new HashSet<>();
            String memberId;
            String memberName;
            int count = 0;

            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                memberId = document.getString("member_id");
                memberName = document.getString("member_name");

                String finalMemberId = memberId;

                if (!membersAlreadyEvaluated.contains(memberId)) {

                    futures[0] = parallelExecutor.submit(MongoDbUserOperations::extractMemberDocument, document);

                    futures[2] = parallelExecutor.submit(MongoDbUserOperations::extractCityDocumentToEmbedPerMember,
                            document);
                    futures[1] = parallelExecutor.submit(e -> extractTopicsPerMember(e), memberId);
                    futures[3] = parallelExecutor.submit(e -> extractFutureEventsPerMember(e), memberId);
                    String finalMemberName1 = memberName;

                    finalDocument = (Document) futures[0].get();
                    finalDocument.append("city", futures[2].get());

                    finalDocument.append("upcoming_events", futures[3].get());
                    finalDocument.append("topics", futures[1].get());

                    finalDocument.append("event_count",
                            eventCount.get(memberId) != null ? eventCount.get(memberId) : 0);

                    finalDocument.append("password", new BCryptPasswordEncoder().encode(memberName + "_password"));
                    finalDocument.append("isAdmin", count < 5);

                    //                    List<String> groups_organizer= new ArrayList<>();
                    //                    if (MongoDbGroupOperations.groups_per_organizer.get(memberId)!=null) {
                    //                      groups_organizer = MongoDbGroupOperations.groups_per_organizer.get(memberId);
                    //                    }
                    finalDocument.append("group_count",
                            (groupCount.get(memberId) != null ? groupCount.get(memberId) : 0));
                    Document finalDocument1 = finalDocument;
                    parallelExecutor.submit(() -> newMemberCollection.insertOne(finalDocument1));

                    membersAlreadyEvaluated.add(memberId);
                    count++;
                }

            }
        }
    }

    public List<Document> getMembersWithMemberId(String member_id) {
        List<Document> documentList = new ArrayList<>();
        MongoCollection oldMemberCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
        try (MongoCursor mongoCursor = oldMemberCollection.find(Filters.eq("member_id", member_id)).cursor()) {
            while (mongoCursor.hasNext()) {
                Document document = (Document) mongoCursor.next();
                documentList.add(document);
            }
        }
        return documentList;
    }

    public static List retrieveIdsFromMetaMembers() {
        MongoCollection metaMembersCollection = MongoDataLoader.csvDocuments.getCollection("meta-members.csv");
        metaMembersCollection.createIndex(new Document("member_id", 1));
        List<String> IdsFromMetaMembersColl = new ArrayList<>();

        MongoCursor mongoCursor = metaMembersCollection.find()
                .projection(new Document("member_id", true).append("_id", false)).cursor();
        if (mongoCursor != null) {
            while (mongoCursor.hasNext()) {
                IdsFromMetaMembersColl.add(((Document) mongoCursor.next()).get("member_id", String.class));
            }
        }
        return IdsFromMetaMembersColl;
    }

    public static List retrieveIdsFromMembers(int limit) {
        MongoCollection metaMembersCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
        List<String> IdsFromMetaMembersColl = new ArrayList<>();

        MongoCursor mongoCursor = metaMembersCollection.find()
                .projection(new Document("member_id", true).append("_id", false)).cursor();
        if (mongoCursor != null) {
            int count = 0;
            while (mongoCursor.hasNext()) {
                IdsFromMetaMembersColl.add(((Document) mongoCursor.next()).get("member_id", String.class));
                count++;
                if (count >= limit && limit != -1) break;
            }
        }
        return IdsFromMetaMembersColl;
    }

    public static List updateIdsForMembers() {
        MongoCollection membersCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
        MongoCollection memberTopicsCollection = MongoDataLoader.csvDocuments.getCollection("members_topics.csv");
        membersCollection.createIndex(new Document("member_id", 1));
        memberTopicsCollection.createIndex(new Document("member_id", 1));
        List<String> IdsFromMetaMembersColl = retrieveIdsFromMetaMembers();
        int totalMetaMembers = IdsFromMetaMembersColl.size();
        //       List<String> membersId = retrieveIdsFromMembers(-1);

        int membersChosen = 0;
        List<Document> aggregationList = Arrays.asList(
                new Document("$group",
                        new Document("_id", "$member_id")
                                .append("firstMember",
                                        new Document("$first", "$member_id"))),
                new Document("$limit", totalMetaMembers));

        try (MongoCursor<Document> mongoCursor = membersCollection.aggregate(aggregationList).cursor()) {

            Set<String> chosenMembers = new HashSet<>();
            chosenMembers.addAll(IdsFromMetaMembersColl);

            int value = -1;
            String chosenMember;
/*
            do {
                chosenRecord = destinationIds.get(random.nextInt(totalDesinationRecords));
            }
            while (chosenRecords.contains(chosenRecord));

            chosenRecords.add(chosenRecord);
 */
            while (membersChosen <= totalMetaMembers && mongoCursor.hasNext()) {
                String memberIdToUpdate;
                do {
                    memberIdToUpdate = mongoCursor.next().getString("_id");
                } while (chosenMembers.contains(memberIdToUpdate) && mongoCursor.hasNext());

                chosenMembers.add(memberIdToUpdate);

                String finalMemberIdToUpdate = memberIdToUpdate;
                int finalMembersChosen = membersChosen;
                Thread thread1 = new Thread(() -> {
                    membersCollection.updateMany(
                            Filters.eq("member_id", finalMemberIdToUpdate),
                            new Document("$set",
                                    new Document("member_id", IdsFromMetaMembersColl.get(finalMembersChosen))
                            )
                    );
                });
                String finalMemberIdToUpdate1 = memberIdToUpdate;
                int finalMembersChosen1 = membersChosen;
                Thread thread2 = new Thread(() -> {
                    memberTopicsCollection.updateMany(Filters.eq("member_id", finalMemberIdToUpdate1),
                            new Document("$set",
                                    new Document("member_id", IdsFromMetaMembersColl.get(finalMembersChosen1))
                            ));
                });
                thread1.start();
                thread2.start();
                thread1.join();
                thread2.join();

                membersChosen++;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        metaMembersIds = IdsFromMetaMembersColl;
        return IdsFromMetaMembersColl;
    }

    //    public static long countUpdatedMember(){
    //      List<String> list =  retrieveIdsFromMetaMembers();
    //      MongoCollection mongoCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
    //      long count = mongoCollection.countDocuments(Filters.in("member_id",list));
    //      return count;
    //    }

}
