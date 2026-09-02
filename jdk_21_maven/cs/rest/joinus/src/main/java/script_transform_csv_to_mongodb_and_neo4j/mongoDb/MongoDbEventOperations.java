package script_transform_csv_to_mongodb_and_neo4j.mongoDb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Future;


public class MongoDbEventOperations {

    public MongoClient mongoClient;
    public MongoDatabase mongoOriginalDatabase;
    public static String newEventCollectionName = "events";
    private final ParallelExecutor parallelExecutor;

    public MongoCollection getNewEventCollection() {
        mongoOriginalDatabase.createCollection(newEventCollectionName);
        return mongoOriginalDatabase.getCollection(newEventCollectionName);
    }

    public MongoDbEventOperations(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoDbEventOperations(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            MongoCollection eventCollection, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoDbEventOperations(MongoClient mongoClient, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        mongoOriginalDatabase = mongoClient.getDatabase("joinUs");
        this.parallelExecutor = parallelExecutor;
    }

    public static Document extractFeeFromEvent(Document oldEvent) {

        Document feeDocument = new Document();
        feeDocument.append("accepts", oldEvent.getString("fee_accepts"));
        String amount = oldEvent.getString("fee_amount");
        if (amount != null && !amount.isEmpty() && !amount.isBlank()) {
            feeDocument.append("amount", Double.parseDouble(amount));
        } else feeDocument.append("amount", 0);

        String currency = oldEvent.getString("fee_currency");

        if (!currency.equalsIgnoreCase("not_found")) {
            feeDocument.append("currency", currency);
        }
        feeDocument.append("description", oldEvent.getString("fee_description"));

        String isFeeRequired = oldEvent.getString("fee_required");
        if (isFeeRequired.equalsIgnoreCase("0")) feeDocument.append("isRequired", false);
        else feeDocument.append("isRequired", true);

        return feeDocument;
    }

    /**
     * Joining groups by name here to be safer ,since we are going to change the ids
     *
     * @param oldEvent
     * @return
     */
    public Document extractCreatorGroupForEvent(Document oldEvent) {
        Document groupDocument = new Document();
        String event_id = oldEvent.getString("event_id");
        MongoCollection groupCollection = MongoDataLoader.csvDocuments.getCollection("groups.csv");

        String groupName = oldEvent.getString("group_name");

        Document document = (Document) groupCollection.find(Filters.eq("group_name", groupName)).first();

        if (document == null) return null;

        groupDocument.append("group_id", document.getString("group_id"));
        groupDocument.append("group_name", document.getString("group_name"));
        groupDocument.append("thumb_link", document.getString("group_photo_thumb_link"));

        return groupDocument;

    }

    public static Document extractVenueForEvent(Document oldEvent) {
        String cityName = oldEvent.getString("venue_city");
        Document venue = new Document();

        Document city = MongoDbCityOperation.extractCityToEmbedFromCityName(cityName);
        venue.append("city", city);
        MongoDataLoader.assignIfFound(venue, "address_1", oldEvent.getString("venue_address_1"));
        MongoDataLoader.assignIfFound(venue, "address_2", oldEvent.getString("venue_address_2"));

        return venue;

    }

    public static Document extractEventDocument(Document oldEvent) {

        Document event = new Document();

        List<String> fieldsToIncludeDirectly = List.of(
                "description", "event_name");

        event.append("_id", oldEvent.getString("event_id"));
        for (String key : oldEvent.keySet()) {
            if (fieldsToIncludeDirectly.contains(key)) {
                event.append(key, oldEvent.getString(key));
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date created = null;
        Date event_time = null;
        Date updated = null;
        try {
            created = simpleDateFormat.parse(oldEvent.getString("created"));
            event_time = simpleDateFormat.parse(oldEvent.getString("event_time"));
            updated = simpleDateFormat.parse(oldEvent.getString("updated"));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Instant eventTimeInstant = event_time.toInstant();
        Instant updatedInstant = updated.toInstant();
        Instant createdInstant = created.toInstant();

        //Adding 9 years to simulate "upcoming events" since currently the latest date in the data is 2018
        //TODO modify accordingly
        event_time = Date.from(eventTimeInstant.plus(9 * 365 + 2, ChronoUnit.DAYS));
        updated = Date.from(updatedInstant.plus(9 * 365 + 2, ChronoUnit.DAYS));
        created = Date.from(createdInstant.plus(9 * 365 + 2, ChronoUnit.DAYS));

        event.append("created", created);
        event.append("event_time", event_time);
        event.append("updated", updated);

        Long duration = Long.parseLong(oldEvent.getString("duration"));

        event.append("duration", duration / 60);

        return event;
    }

    /**
     * For now this method gets the category directly from the Group
     *
     * @param oldEvent
     * @return
     */
    public static Document extractCategoryForEvent(Document oldEvent) {
        Document eventDocument = new Document();

        MongoCollection groupCollection = MongoDataLoader.csvDocuments.getCollection("groups.csv");

        Document group = (Document) groupCollection.find(Filters.eq("group_name", oldEvent.getString("group_name")))
                .first();

        return MongoDbGroupOperations.extractCategoryFromGroup(group);
    }

    public HashMap<String, Double> extractMemberCountForEvent() {
        HashMap<String, Double> memberCount = new HashMap<>();
        MongoCollection rsvpsCollection = MongoDataLoader.csvDocuments.getCollection("rsvps.csv");

        List<Document> aggregationList = Arrays.asList(new Document("$group",
                        new Document("_id", "$event_id")
                                .append("member_count",
                                        new Document("$sum", 1L))),
                new Document("$project",
                        new Document("event_id", "$_id")
                                .append("member_count",
                                        new Document("$toDouble", "$member_count"))));

        MongoCursor<Document> mongoCursor = rsvpsCollection.aggregate(aggregationList).cursor();

        while (mongoCursor.hasNext()) {
            Document document = mongoCursor.next();
            memberCount.put(document.getString("event_id"),
                    document.getDouble("member_count") != null ? document.getDouble("member_count") : 0);
        }
        return memberCount;

    }

    public void createEventCollection() {

        MongoCollection newEventCollection = getNewEventCollection();

        HashMap<String, Double> memberCount = extractMemberCountForEvent();

        MongoCollection oldEventCollection = MongoDataLoader.csvDocuments.getCollection("events.csv");

        try (MongoCursor<Document> mongoCursor = oldEventCollection.find().cursor()) {
            Future[] futures = new Future[5];

            Document newEvent;
            while (mongoCursor.hasNext()) {
                Document oldDocument = mongoCursor.next();
                String event_id = oldDocument.getString("event_id");
                String event_name = oldDocument.getString("event_name");

                if (event_name == null || event_name.isEmpty() || event_name.isBlank()) continue;

                futures[0] = parallelExecutor.submit(MongoDbEventOperations::extractEventDocument, oldDocument);
                futures[1] = parallelExecutor.submit(MongoDbEventOperations::extractCategoryForEvent, oldDocument);
                futures[2] = parallelExecutor.submit(MongoDbEventOperations::extractFeeFromEvent, oldDocument);
                futures[3] = parallelExecutor.submit(MongoDbEventOperations::extractVenueForEvent, oldDocument);
                futures[4] = parallelExecutor.submit(e -> extractCreatorGroupForEvent(e), oldDocument);

                newEvent = (Document) futures[0].get();

                newEvent.append("category", futures[1].get());
                newEvent.append("fee", futures[2].get());
                newEvent.append("venue", futures[3].get());

                MongoDataLoader.assignIfFound(newEvent, "creator_group", futures[4].get());
                newEvent.append("member_count", memberCount.get(event_id) != null ? memberCount.get(event_id) : 0);

                //                newEvent = extractEventDocument(oldDocument);
                //                CsvToMongoTransformer.assignIfFound(newEvent, "creator_group", extractCreatorGroupForEvent(oldDocument));
                //                newEvent.append("categories", extractCategoryForEvent(oldDocument));
                //                newEvent.append("fee", extractFeeFromEvent(oldDocument));
                //                newEvent.append("venue", extractVenueForEvent(oldDocument));
                //                newEvent.append("member_count", memberCount.get(event_id));

                Document finalNewEvent = newEvent;
                parallelExecutor.submit(() -> newEventCollection.insertOne(finalNewEvent));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Document> extractUpcomingEventsToEmbed(List<String> event_ids) {
        List<Document> upcomingEvents = new ArrayList<>();
        MongoCollection<Document> eventCollection = MongoDataLoader.newMongoDatabase.getCollection("events");

        try (MongoCursor<Document> eventCursor = eventCollection.find(
                Filters.and(
                        Filters.in("_id", event_ids)
                        , Filters.gt("event_time", new Date())
                )
        ).cursor()) {

            while (eventCursor.hasNext()) {
                Document documentToEmbed = new Document();
                Document eventDocument = eventCursor.next();
                Date event_time = eventDocument.getDate("event_time");

                //            if (event_time.after(new Date())) {
                documentToEmbed.append("event_id", eventDocument.getString("_id"));
                documentToEmbed.append("event_name", eventDocument.getString("event_name"));
                documentToEmbed.append("event_time", event_time);
                upcomingEvents.add(documentToEmbed);
                //            }
            }
        }
        return upcomingEvents;
    }

}
