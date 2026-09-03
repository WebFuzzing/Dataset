package script_transform_csv_to_mongodb_and_neo4j.mongoDb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import script_transform_csv_to_mongodb_and_neo4j.ConfigurationFileReader;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;


public class MongoDbGroupOperations {

    public MongoClient mongoClient;
    public MongoDatabase mongoOriginalDatabase;
    public static String newGroupCollectionName = "groups";
    private final ParallelExecutor parallelExecutor;
    public static HashMap<String, List<String>> groups_per_organizer = new HashMap<>();

    public MongoCollection getNewGroupsCollection() {
        mongoOriginalDatabase.createCollection(newGroupCollectionName);
        return mongoOriginalDatabase.getCollection(newGroupCollectionName);
    }

    public MongoDbGroupOperations(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoDbGroupOperations(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            MongoCollection eventCollection, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoDbGroupOperations(MongoClient mongoClient, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        mongoOriginalDatabase = mongoClient.getDatabase(ConfigurationFileReader.getMongoDatabase());
        this.parallelExecutor = parallelExecutor;
    }

    public static Document extractCategoryFromGroup(Document oldGroupDocument) {
        if (oldGroupDocument == null) {
            return new Document();
        }
        Document category = new Document();

        category.append("category_id", oldGroupDocument.getString("category_id"))
                .append("name", oldGroupDocument.getString("category_name"));

        return category;
    }

    /**
     * Adds members from the first dataset groups.csv ("members") and counts members from the second dataset rsvps.csv
     */
    public HashMap<String, Double> extractMemberCount() {

        HashMap<String, Double> memberCount = new HashMap<>();

        int membersFromRsvps = 0;
        //rsvps.csv
        MongoCollection rsvpsCollection = MongoDataLoader.csvDocuments.getCollection("rsvps.csv");

        List<Document> aggregationList = Arrays.asList(
                new Document("$group",
                        new Document("_id", "$group_id")
                                .append("member_count",
                                        new Document("$addToSet", "$member_id"))),
                new Document("$project",
                        new Document("group_id", "$_id")
                                .append("member_count",
                                        new Document("$size", "$member_count"))
                                .append("total_Member", 1L)));

        try (MongoCursor<Document> mongoCursor = rsvpsCollection.aggregate(aggregationList).cursor()) {

            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();

                memberCount.put(document.getString("group_id"), Double.parseDouble(document.get("member_count") + ""));
            }
        }

        MongoCollection membersCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");

        List<Document> aggregationOnMembers = Arrays.asList(new Document("$group",
                        new Document("_id", "$group_id")
                                .append("member_count",
                                        new Document("$sum", 1L))),
                new Document("$project",
                        new Document("group_id", "$_id")
                                .append("member_count", 1L)));

        try (MongoCursor<Document> cursor = membersCollection.aggregate(aggregationOnMembers).cursor()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();

                if (!memberCount.containsKey(document.getString("group_id"))) {
                    memberCount.put(document.getString("group_id"),
                            Double.parseDouble(document.get("member_count") + ""));
                }
            }
        }

        return memberCount;

    }

    public List<Document> extractUpcomingEventsFromEvents_csv(String group_id) {
        MongoCollection eventsCollection = MongoDataLoader.csvDocuments.getCollection("events.csv");
        List<String> eventIdsFromEvents = new ArrayList<>();
        try (MongoCursor<Document> cursor = eventsCollection.find(Filters.eq("group_id", group_id)).cursor()) {
            while (cursor.hasNext()) {
                String event_id = (cursor.next()).getString("event_id");
                eventIdsFromEvents.add(event_id);
            }
        }

        return MongoDbEventOperations.extractUpcomingEventsToEmbed(eventIdsFromEvents);

    }

    public List<Document> extractUpcomingEventsFromRSVPS_csv(String group_id) {
        //rsvps.csv
        MongoCollection rsvpsCollection = MongoDataLoader.csvDocuments.getCollection("rsvps.csv");
        List<String> eventIdsFromRsvps = new ArrayList<>();
        try (MongoCursor<Document> cursor = rsvpsCollection.find(Filters.eq("group_id", group_id)).cursor()) {

            while (cursor.hasNext()) {
                String event_id = (cursor.next()).getString("event_id");
                eventIdsFromRsvps.add(event_id);
            }
        }

        return MongoDbEventOperations.extractUpcomingEventsToEmbed(eventIdsFromRsvps);
    }

    public List<Document> extractUpcomingEvents(Document oldGroupDocument) {

        if (oldGroupDocument == null || oldGroupDocument.isEmpty()) return new ArrayList<>();

        String group_id = oldGroupDocument.getString("group_id");

        List<Document> upcomingEvents = new ArrayList<>();
        upcomingEvents.addAll(extractUpcomingEventsFromRSVPS_csv(group_id));
        upcomingEvents.addAll(extractUpcomingEventsFromEvents_csv(group_id));
        return upcomingEvents;
    }

    //We can use this method if we want to do the same as for the user (although we have categories for the aggregations)
    public List<Document> extractTopicsForGroups(Document oldGroupDocument) {
        if (oldGroupDocument == null || oldGroupDocument.isEmpty()) return new ArrayList<>();

        String group_id = oldGroupDocument.getString("group_id");
        MongoCollection topicCollection = MongoDataLoader.csvDocuments.getCollection("groups_topics.csv");

        List<Document> groupTopics = new ArrayList<>();
        MongoCursor cursor = topicCollection
                .find(new Document("group_id", group_id))
                .projection(new Document("topic_id", 1).append("topic_key", 1))
                .cursor();
        while (cursor.hasNext()) {
            groupTopics.add((Document) cursor.next());
        }

        return groupTopics;
    }

    public static Document extractCityForGroup(Document oldGroupDocument) {
        //        String group_id=oldGroupDocument.getString("group_id");
        String cityName = oldGroupDocument.getString("city");
        return MongoDbCityOperation.extractCityToEmbedFromCityName(cityName);
    }

    public HashMap<String, Double> extractEventCount() {
        HashMap<String, Double> eventCounts = new HashMap<>();

        extractEventCountFromRsvps_csv(eventCounts);
        extractEventCountFromEvents_csv(eventCounts);

        return eventCounts;
    }

    public void extractEventCountFromRsvps_csv(HashMap<String, Double> eventCount) {
        MongoCollection rsvpsCollection = MongoDataLoader.csvDocuments.getCollection("rsvps.csv");

        List<Document> aggregationList = Arrays.asList(new Document("$group",
                        new Document("_id", "$group_id")
                                .append("eventCount",
                                        new Document("$addToSet", "$event_id"))),
                new Document("$project",
                        new Document("group_id", "$_id")
                                .append("eventCount",
                                        new Document("$size", "$eventCount"))));

        try (MongoCursor<Document> mongoCursor = rsvpsCollection.aggregate(aggregationList).cursor()) {

            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                eventCount.put(document.getString("group_id"),
                        Double.parseDouble(String.valueOf(document.get("eventCount"))));

            }
        }
    }

    public void extractEventCountFromEvents_csv(HashMap<String, Double> eventCount) {

        MongoCollection eventsCollection = MongoDataLoader.csvDocuments.getCollection("events.csv");

        List<Document> aggregationList = List.of(
                new Document("$group",
                        new Document("_id", "$group_id")
                                .append("total_events",
                                        new Document("$sum", 1L))));

        try (MongoCursor<Document> mongoCursor = eventsCollection.aggregate(aggregationList).cursor()) {

            while (mongoCursor.hasNext()) {
                Document document = mongoCursor.next();
                String group_id = document.getString("_id");
                if (!eventCount.containsKey(group_id))
                    eventCount.put(group_id, Double.parseDouble(String.valueOf(document.get("total_events"))));
            }
        }

    }

    public List<Document> extractOrganizer(Document oldGroupDocument) {
        List<Document> organizers = new ArrayList<>();
        String group_id = oldGroupDocument.getString("group_id");
        Document organizer = new Document();
        //        String organizerName=oldGroupDocument.getString("organizer_name");
        //        organizer.append("member_name",organizerName);
        String organizerId = oldGroupDocument.getString("organizer_member_id");

        MongoCollection mongoCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
        MongoCursor<Document> mongoCursor = mongoCollection.find(Filters.eq("member_id", organizerId)).cursor();

        if (mongoCursor.hasNext()) {
            organizer.append("member_id", organizerId);
            organizer.append("member_name", mongoCursor.next().getString("member_name"));
        }
        //        if (groups_per_organizer.containsKey(organizerId)){
        //            groups_per_organizer.get(organizerId).add(group_id);
        //        }
        //        else {
        //            groups_per_organizer.put(organizerId,new ArrayList<>());
        //        }

        organizers.add(organizer);

        return organizers;
    }

    public List<Document> extractTopicsPerGroup(String groupId) {

        MongoCollection topicCollection = MongoDataLoader.csvDocuments.getCollection("groups_topics.csv");

        List<Document> groupTopics = new ArrayList<>();
        try (MongoCursor<Document> cursor = topicCollection
                .find(new Document("group_id", groupId))
                .cursor()) {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                Document documentToAdd = new Document();
                documentToAdd.append("topic_id", document.getString("topic_id"));
                documentToAdd.append("topic_name", document.getString("topic_name"));
                groupTopics.add(documentToAdd);
            }
        }

        return groupTopics;
    }

    public static Document extractGroupDocument(Document oldGroupDocument) {
        List<String> directKeysToInclude = List.of(
                "group_name");
        Document newGroupDocument = new Document();
        for (String key : oldGroupDocument.keySet()) {
            if (directKeysToInclude.contains(key)) {
                newGroupDocument.append(key, oldGroupDocument.getString(key));
            }
        }
        newGroupDocument.append("_id", oldGroupDocument.getString("group_id"));
        MongoDataLoader.assignIfFound(newGroupDocument, "description", oldGroupDocument.getString("description"));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date created = null;
        try {
            created = simpleDateFormat.parse(oldGroupDocument.getString("created"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        newGroupDocument.append("created", created);

        //        Double rating = Double.parseDouble(oldGroupDocument.getString("rating"));
        //        Long utcOffset = Long.parseLong(oldGroupDocument.getString("utc_offset"));
        //        newGroupDocument.append("rating",rating);
        //        newGroupDocument.append("utc_offset",utcOffset);

        return newGroupDocument;
    }

    public static Document extractGroupPhoto(Document oldGroupDocument) {
        Document photo = new Document();
        for (String key : oldGroupDocument.keySet()) {
            if (key.startsWith("group_photo_")) {
                String newKey = key.substring("group_photo_".length());
                if (!newKey.equalsIgnoreCase("type")
                        && !newKey.equalsIgnoreCase("base_url")
                        && !newKey.equalsIgnoreCase("photo_id")) {
                    //group_photo type, base_url and photo
                    MongoDataLoader.assignIfFound(photo, newKey, oldGroupDocument.getString(key));
                }
            }
        }
        return photo;
    }

    public void createGroupCollection() throws Exception {

        MongoCollection groupCollection = getNewGroupsCollection();
        MongoCollection categoryNewCollection = MongoDataLoader.newMongoDatabase.getCollection("categories");

        List<String> categoriesIncluded = new ArrayList<>();

        try (MongoCursor<Document> mongoCursor = MongoDataLoader.csvDocuments.getCollection("groups.csv")
                .find()
                .cursor()) {

            HashMap<String, Double> memberCount = extractMemberCount();
            HashMap<String, Double> eventCount = extractEventCount();

            while (mongoCursor.hasNext()) {

                Document oldGroupDocument = mongoCursor.next();
                Future[] futures = new Future[8];
                Document newGroupDocument = new Document();

                String group_id = oldGroupDocument.getString("group_id");

                String category_id = oldGroupDocument.getString("category_id");

                if (!categoriesIncluded.contains(category_id)) {
                    String name = oldGroupDocument.getString("category_name");
                    Document categoryDocument = new Document();
                    categoryDocument.append("_id", category_id);
                    categoryDocument.append("name", name);

                    parallelExecutor.submit(() -> categoryNewCollection.insertOne(categoryDocument));
                    categoriesIncluded.add(category_id);
                }

                futures[0] = parallelExecutor.submit(MongoDbGroupOperations::extractGroupDocument, oldGroupDocument);
                futures[1] = parallelExecutor.submit(MongoDbGroupOperations::extractCityForGroup, oldGroupDocument);
                futures[2] = parallelExecutor.submit(MongoDbGroupOperations::extractCategoryFromGroup,
                        oldGroupDocument);

                futures[3] = parallelExecutor.submit(e -> extractOrganizer(e), oldGroupDocument);
                futures[4] = parallelExecutor.submit(e -> extractUpcomingEvents(e), oldGroupDocument);
                futures[5] = parallelExecutor.submit(MongoDbGroupOperations::extractGroupPhoto, oldGroupDocument);
                futures[6] = parallelExecutor.submit(e -> extractTopicsPerGroup(e), group_id);

                newGroupDocument = (Document) futures[0].get();
                newGroupDocument.append("city", futures[1].get());
                newGroupDocument.append("category", futures[2].get());

                Document finalNewGroupDocument1 = newGroupDocument;

                newGroupDocument.append("organizers", futures[3].get());
                newGroupDocument.append("topics", new Document());
                newGroupDocument.append("upcoming_events", futures[4].get());
                newGroupDocument.append("group_photo", futures[5].get());
                newGroupDocument.append("event_count", eventCount.get(group_id) != null ? eventCount.get(group_id) : 0);
                newGroupDocument.append("member_count",
                        memberCount.get(group_id) != null ? memberCount.get(group_id) : 0);
                newGroupDocument.append("topics", futures[6].get());//might do this if we want

                //                newGroupDocument = extractGroupDocument(oldGroupDocument);
                //                newGroupDocument.append("city", extractCityForGroup(oldGroupDocument));
                //                newGroupDocument.append("categories", extractCategoryFromGroup(oldGroupDocument));
                //                newGroupDocument.append("event_count", eventCount.get(group_id));
                //                newGroupDocument.append("member_count", memberCount.get(group_id));
                //                newGroupDocument.append("organizer_members", extractOrganizer(oldGroupDocument));
                //                newGroupDocument.append("upcoming_events", extractUpcomingEvents(oldGroupDocument));
                //                newGroupDocument.append("group_photo", extractGroupPhoto(oldGroupDocument));
                //                newGroupDocument.append("topics",extractTopicsForGroups(oldGroupDocument));//might do this if we want

                Document finalNewGroupDocument = newGroupDocument;

                parallelExecutor.submit(() -> groupCollection.insertOne(finalNewGroupDocument));
            }
        }

    }

}
