package script_transform_csv_to_mongodb_and_neo4j.mongoDb;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.opencsv.CSVReader;
import org.bson.Document;
import org.bson.conversions.Bson;
import script_transform_csv_to_mongodb_and_neo4j.ConfigurationFileReader;
import script_transform_csv_to_mongodb_and_neo4j.CsvDataOperations;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static script_transform_csv_to_mongodb_and_neo4j.CsvDataOperations.membersLimit;


public class MongoDataLoader {

    public static MongoClient client = MongoClients.create(ConfigurationFileReader.getMongoUrl());
    public static final String firstDatasetFolder = ConfigurationFileReader.checkAndGetProp(
            "firstDatasetFolder");// PATH OF THE DATASET FOUND BY MOIN
    public static final String secondDatasetFolder = ConfigurationFileReader.checkAndGetProp(
            "secondDatasetFolder");//PATH OF THE DATASET FOUND BY FLORIAN
    public static final MongoDatabase csvDocuments = client.getDatabase("CSV_Documents");
    public static final MongoDatabase newMongoDatabase = client.getDatabase(ConfigurationFileReader.getMongoDatabase());
    private static final int BATCH_SIZE = 1000;
    private final ParallelExecutor parallelExecutor;
    public static boolean useCsvDataUpdater = true;

    // if true update data directly in CSV before importing , else update through mongodb

    public MongoDataLoader(ParallelExecutor parallelExecutor) {
        this.parallelExecutor = parallelExecutor;
    }

    public void transformCsvDataToMongoDB() throws Exception {
        long startingTime = System.currentTimeMillis();

        // Step-1 , import all the csv data to MongoDB as it is (replace '.' with '_' in the fields that contain '.' due to issues with MongoDB)

        //
        System.out.println("IMPORTING THE CSV DATA INTO MongoDB !!!! ");

        importCsvToMongoDB(firstDatasetFolder, membersLimit);
        importCsvToMongoDB(secondDatasetFolder, membersLimit);

        if (useCsvDataUpdater) importCsvToMongoDB(CsvDataOperations.transformedDatasetFolder, membersLimit);

        //
        System.out.println("FINISHED IMPORTING THE CSV DATA INTO MongoDB !!!! ");

        //        if (!useCsvDataUpdater) updateIdsFromMongoDb();

        createIndexesForCsvCollections();

        //Step-2 ,Create the new Collection we will use for our project , in the new database 'JoinUs'

        ArrayList<Future> futureArrayList = new ArrayList<>();
        //
        System.out.println("NOW READY TO CREATE THE NEW DATABASE !!!! ");

        System.out.println("CREATING THE Events COLLECTION : ");

        startingTime = System.currentTimeMillis();

        //        futureArrayList.add( parallelExecutor.submit(() ->   {
        //            try {
        new MongoDbEventOperations(client, newMongoDatabase, parallelExecutor).createEventCollection();
        createIndexesForEventsCollection();
        //            } catch (Exception e) {
        //                throw new RuntimeException(e);
        //            }
        System.out.println("Finished creating Events Collection");
        //        }));

        futureArrayList.add(parallelExecutor.submit(() -> {
            try {

                new MongoDbUserOperations(client, newMongoDatabase, parallelExecutor)
                        .createMemberCollection(true);
                System.out.println("Finished creating Member Collection");
                createIndexesForMembersCollection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        futureArrayList.add(parallelExecutor.submit(() -> {
            try {
                new MongoDbGroupOperations(client, newMongoDatabase, parallelExecutor).createGroupCollection();
                System.out.println("Finished creating Group Collection");
                createIndexesForGroupsCollection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        futureArrayList.add(parallelExecutor.submit(() -> {

                    new MongoDbTopicOperation(client, newMongoDatabase, parallelExecutor).createTopicCollection();
                    createIndexesForTopicsCollection();
                }
        ));

        futureArrayList.add(parallelExecutor.submit(() -> {

            new MongoDbCityOperation(client, newMongoDatabase, parallelExecutor).createCityCollection();
            createIndexesForCitiesCollections();

        }));

        ParallelExecutor.getFutures(futureArrayList);

        long endingTime = System.currentTimeMillis();

        System.out.println("THE  PROCESS TOOK " + (endingTime - startingTime) / 1000 + " seconds ");

    }

    public static Document renameField(Document document, String oldField, String newField) {
        if (document == null || document.isEmpty()) return document;
        Document newDocument = new Document();
        for (String key : document.keySet()) {
            if (key.equalsIgnoreCase(oldField)) {
                newDocument.append(newField, document.get(key));
            } else {
                newDocument.append(key, document.get(key));
            }
        }
        return newDocument;
    }

    public static Map<String, Object> flattenDocument(
            Object value,
            String parentKey,
            String delimiter
    ) {
        Map<String, Object> flattenedMap = new HashMap<>();

        if (value instanceof Document document) {

            for (Map.Entry<String, Object> entry : document.entrySet()) {
                String newKey = parentKey.isEmpty()
                        ? entry.getKey()
                        : parentKey + delimiter + entry.getKey();

                flattenedMap.putAll(
                        flattenDocument(entry.getValue(), newKey, delimiter)
                );
            }

        } else if (value instanceof List<?>) {
            // Same behavior as JsonNode array → store as string
            flattenedMap.put(parentKey, value.toString());

        } else {
            // Primitive or other BSON type
            flattenedMap.put(parentKey, value);
        }

        return flattenedMap;
    }

    public void updateIdsFromMongoDb() {

        parallelExecutor.submit(() -> {

            updateIdsForCollection(parallelExecutor, "meta-events.csv", "event_id",
                    "event_id", "events.csv");
            System.out.println("UPDATED THE COLLECTION : updateIdsForCollection.events");

        });

        parallelExecutor.submit(() -> {
            MongoDbUserOperations.updateIdsForMembers();
            //
            //            updateIdsForCollection("meta-members.csv","member_id","member_id","members.csv");
            System.out.println("UPDATED THE COLLECTION : updateIdsForMembers.members");
            //
        });

        parallelExecutor.submit(() -> {
            //
            updateIdsForCollection(parallelExecutor, "meta-groups.csv", "group_id",
                    "group_id", "groups.csv", "groups_topics.csv", "events.csv", "members.csv");//
            System.out.println("UPDATED THE COLLECTION : updateIdsForCollection.groups");

        });

    }

    /**
     * @param sourceCollection
     *         The meta collection from which we will get the id
     * @param sourceKey
     * @param destinationCollections
     *         All the collections which need to update the key (id) to what has been chosen from sourceCollection for
     *         example (meta-groups ---> group_id  -->   group.csv, groups_topics.csv , events.csv)
     * @param destinationKey
     *         The key needs to have the same name for all the destinationCollections , e.g. group_id
     * @return
     */
    public static void updateIdsForCollection(ParallelExecutor parallelExecutor, String sourceCollection,
            String sourceKey,
            String destinationKey, String... destinationCollections) {
        MongoCollection sourceColl = MongoDataLoader.csvDocuments.getCollection(sourceCollection);
        sourceColl.createIndex(new Document(sourceKey, 1));
        if (destinationCollections == null || destinationCollections.length < 1)
            throw new RuntimeException("Please provide a destinationCollection");

        List<MongoCollection> destinationColls = new ArrayList<>();

        for (String destColl : destinationCollections) {
            destinationColls.add(MongoDataLoader.csvDocuments.getCollection(destColl));
        }
        //        MongoCollection destColl= CsvToMongoTransformer.csvDocuments.getCollection(destinationCollection);

        List<String> IdsFromSourceColl = retrieveIds(sourceCollection, sourceKey);

        int totalSourceRecords = IdsFromSourceColl.size();
        List<String> destinationIds = retrieveIds(destinationCollections[0], destinationKey);
        int totalDestinationRecords = destinationIds.size();

        Set<String> chosenRecords = new HashSet<>();

        Random random = new Random();
        String chosenRecord;

        int idsChosen = 0;

        while (idsChosen < Math.min(totalSourceRecords, totalDestinationRecords)) {
            do {
                chosenRecord = destinationIds.get(random.nextInt(totalDestinationRecords));
            }
            while (chosenRecords.contains(chosenRecord));

            chosenRecords.add(chosenRecord);

            List<Thread> threadList = new ArrayList<>();
            //Updating the key for all the collections
            //            for (MongoCollection destinationCollection : destinationColls) {

            Future[] futures = new Future[destinationCollections.length];
            int i = 0;
            for (String destCollection : destinationCollections) {
                MongoCollection<Document> destinationCollection = MongoDataLoader.csvDocuments.getCollection(
                        destCollection);

                String finalChosenRecord = chosenRecord;
                int finalIdsChosen = idsChosen;
                futures[i++] = parallelExecutor.submit(() -> {
                    destinationCollection.updateMany(
                            Filters.eq(destinationKey, finalChosenRecord),
                            new Document("$set",
                                    new Document(destinationKey, IdsFromSourceColl.get(finalIdsChosen))
                            )
                    );
                });
            }
            for (Future future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            idsChosen++;

        }
        //        return chosenRecords.stream().toList();
    }

    public static void createIndexesForCsvCollections() {
        MongoCollection metaMembersCollection = MongoDataLoader.csvDocuments.getCollection("meta-members.csv");
        metaMembersCollection.createIndex(new Document("member_id", 1));

        MongoCollection metaEventsCollection = MongoDataLoader.csvDocuments.getCollection("meta-events.csv");
        metaEventsCollection.createIndex(new Document("event_id", 1));

        MongoCollection metaGroupsCollection = MongoDataLoader.csvDocuments.getCollection("meta-groups.csv");
        metaGroupsCollection.createIndex(new Document("group_id", 1));

        MongoCollection membersCollection = MongoDataLoader.csvDocuments.getCollection("members.csv");
        membersCollection.createIndex(new Document("member_id", 1));
        membersCollection.createIndex(new Document("group_id", 1));

        MongoCollection memberTopicsCollection = MongoDataLoader.csvDocuments.getCollection("members_topics.csv");
        memberTopicsCollection.createIndex(new Document("member_id", 1));
        memberTopicsCollection.createIndex(new Document("topic_id", 1));

        MongoCollection rsvpsCollection = MongoDataLoader.csvDocuments.getCollection("rsvps.csv");
        rsvpsCollection.createIndex(new Document("member_id", 1));
        rsvpsCollection.createIndex(new Document("event_id", 1));
        rsvpsCollection.createIndex(new Document("group_id", 1));

        MongoCollection groupsCollection = MongoDataLoader.csvDocuments.getCollection("groups.csv");
        groupsCollection.createIndex(new Document("group_id", 1));
        groupsCollection.createIndex(new Document("group_name", 1));
        groupsCollection.createIndex(new Document("organizer_name", 1));
        groupsCollection.createIndex(new Document("organizer_member_id", 1));

        MongoCollection groupTopicsCollection = MongoDataLoader.csvDocuments.getCollection("groups_topics.csv");
        groupTopicsCollection.createIndex(new Document("group_id", 1));
        groupTopicsCollection.createIndex(new Document("topic_id", 1));
        //
        //group_name
        MongoCollection eventsCollection = MongoDataLoader.csvDocuments.getCollection("events.csv");
        eventsCollection.createIndex(new Document("event_id", 1));
        eventsCollection.createIndex(new Document("event_name", 1));
        eventsCollection.createIndex(new Document("group_id", 1));

        MongoCollection citiesCollection = MongoDataLoader.csvDocuments.getCollection("cities.csv");
        citiesCollection.createIndex(new Document("city", 1));
        citiesCollection.createIndex(new Document("city_id", 1));

    }

    public static void createIndexesForNewCollections() {
        createIndexesForEventsCollection();
        createIndexesForGroupsCollection();
        createIndexesForMembersCollection();
        createIndexesForCitiesCollections();
        createIndexesForCitiesCollections();
    }

    public static void createIndexesForCitiesCollections() {

        MongoCollection citiesCollection = MongoDataLoader.newMongoDatabase.getCollection("cities");
        citiesCollection.createIndex(new Document("name", 1));

    }

    public static void createIndexesForEventsCollection() {
        MongoCollection eventsCollection = MongoDataLoader.newMongoDatabase.getCollection("events");
        eventsCollection.createIndex(new Document("event_id", 1));
        eventsCollection.createIndex(new Document("event_name", 1));
        eventsCollection.createIndex(new Document("creator_group", 1));
        eventsCollection.createIndex(new Document("event_time", 1));
        eventsCollection.createIndex(new Document("fee.amount", 1));
        eventsCollection.createIndex(new Document("venue.city.name", 1));

    }

    public static void createIndexesForMembersCollection() {
        MongoCollection membersCollection = MongoDataLoader.newMongoDatabase.getCollection("members");
        membersCollection.createIndex(new Document("member_id", 1));
    }

    public static void createIndexesForGroupsCollection() {
        MongoCollection groupsCollection = MongoDataLoader.newMongoDatabase.getCollection("groups");
        groupsCollection.createIndex(new Document("group_id", 1));
        groupsCollection.createIndex(new Document("group_name", 1));
        groupsCollection.createIndex(new Document("event_count", 1));
        groupsCollection.createIndex(new Document("member_count", 1));
        groupsCollection.createIndex(new Document("category.name", 1));
        groupsCollection.createIndex(new Document("city.name", 1));

    }

    public static void createIndexesForTopicsCollection() {
        MongoCollection topicsCollection = MongoDataLoader.newMongoDatabase.getCollection("topics");
        topicsCollection.createIndex(new Document("topic_id", 1));
    }

    public static List<String> retrieveIds(String collection, String key) {
        MongoCollection sourceColl = MongoDataLoader.csvDocuments.getCollection(collection);
        List<String> IdsFromSourceColl = new ArrayList<>();

        MongoCursor mongoCursor = sourceColl.find()
                .projection(new Document(key, true).append("_id", false)).cursor();
        if (mongoCursor != null) {
            while (mongoCursor.hasNext()) {
                IdsFromSourceColl.add(((Document) mongoCursor.next()).get(key, String.class));
            }
        }
        return IdsFromSourceColl;
    }

    public static List<String> retrieveIds(String collection, String key, Bson bson) {
        MongoCollection sourceColl = MongoDataLoader.csvDocuments.getCollection(collection);
        List<String> IdsFromSourceColl = new ArrayList<>();

        MongoCursor mongoCursor = sourceColl.find(bson)
                .projection(new Document(key, true).append("_id", false)).cursor();
        if (mongoCursor != null) {
            while (mongoCursor.hasNext()) {
                IdsFromSourceColl.add(((Document) mongoCursor.next()).get(key, String.class));
            }
        }
        return IdsFromSourceColl;
    }

    public static void assignIfFound(Document document, String key, Object value) {
        if (value == null) return;

        if (value instanceof Document document1) {
            if (document1.isEmpty()) return;
        }

        if (value instanceof String val) {
            if (val.equalsIgnoreCase("not_found") || val.isEmpty()) return;
        }

        document.append(key, value);
    }

    //To be used for the creation of Neo4J database ,for easier than working with CSV
    public static void verifyMongoDatabaseAndCollections() throws Exception {
        AtomicBoolean databaseExists = new AtomicBoolean(false);
        client.listDatabaseNames().forEach(e -> {
            if (e.equalsIgnoreCase(ConfigurationFileReader.getMongoDatabase())) {
                databaseExists.set(true);
            }
        });
        if (!databaseExists.get())
            throw new Exception("Database" + MongoDataLoader.newMongoDatabase.getName() + " not created in MongoDb."
            );

        AtomicBoolean allCollectionsCreated = new AtomicBoolean(false);
        List<String> collectionsToVerify = List.of("events", "members", "topics", "groups");

        for (String coll : collectionsToVerify) {
            allCollectionsCreated.set(false);
            client.getDatabase(ConfigurationFileReader.getMongoDatabase()).listCollectionNames().forEach(e -> {
                if (e.equalsIgnoreCase(coll)) {
                    allCollectionsCreated.set(true);
                }
            });
            if (!allCollectionsCreated.get()) throw new Exception("Collection " + coll + " has  not been " +
                    "created in MongoDb.");

        }

    }

    public static Document flattenDocument(Document document) {
        Map<String, Object> flat =
                flattenDocument(document, "", "_");

        return new Document(flat);
    }

    public static Document cloneDocument(Document document) {
        if (document == null) return null;
        Document document1 = new Document();
        for (String key : document.keySet()) {
            document1.append(key, document.get(key));
        }
        return document1;
    }

    /**
     * @param csvPath
     *         folder to find the files to import on MongoDB as they are
     * @throws Exception
     *         The goal of this method is simply to import the csv data on MongoDB , which would still require the
     *         necessary modifications to make sense for MongoDB.
     */
    public static void importCsvToMongoDB(
            String csvPath, double membersCsvLimit
    ) throws Exception {

        if (filterForImportCsvToMongoDBMethod(csvPath)) return;

        if (!Files.isDirectory(Path.of(csvPath))) {
            MongoCollection<Document> collection = csvDocuments.getCollection(
                    csvPath.substring(csvPath.lastIndexOf(File.separator) + 1));

            try (CSVReader reader = new CSVReader(
                    new FileReader(csvPath))) {

                String[] header = reader.readNext(); // first line
                String[] row;

                List<Document> batch = new ArrayList<>();

                double rowCount = 0;
                while ((row = reader.readNext()) != null) {
                    Document doc = new Document();

                    for (int i = 0; i < header.length; i++) {
                        doc.append(header[i].contains(".") ? header[i].replace(".", "_") : header[i],
                                row.length > i ? row[i] : null);
                    }

                    batch.add(doc);

                    if (batch.size() == BATCH_SIZE) {
                        collection.insertMany(batch);
                        batch = new ArrayList<>();
                    }
                    if (csvPath.contains("members.csv") && (membersCsvLimit > 0 && rowCount >= membersCsvLimit)) break;
                    rowCount++;
                }
                if (!batch.isEmpty()) {
                    collection.insertMany(batch);
                }
            }
        } else {
            for (File file : new File(csvPath).listFiles()) {
                importCsvToMongoDB(file.toString(), membersCsvLimit);
            }
        }
    }

    public static boolean filterForImportCsvToMongoDBMethod(String csvPath) {
        if (!Files.isDirectory(Path.of(csvPath))) {
            if (csvPath.contains("edge")) return true;

            if (useCsvDataUpdater && !((new File(csvPath).getParent() + "\\").equalsIgnoreCase(
                    CsvDataOperations.transformedDatasetFolder))) {
                for (File file : new File(CsvDataOperations.transformedDatasetFolder).listFiles()) {
                    if (file.getName().equalsIgnoreCase(csvPath.substring(csvPath.lastIndexOf("\\") + 1))) {
                        return true;
                    }
                }
            }

        }
        return false;
    }
}
