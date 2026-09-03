package script_transform_csv_to_mongodb_and_neo4j.mongoDb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.util.List;


public class MongoDbTopicOperation {

    public MongoClient mongoClient;
    public MongoDatabase mongoOriginalDatabase;
    public static String newTopicCollectionName = "topics";
    private final ParallelExecutor parallelExecutor;

    public MongoDbTopicOperation(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoCollection getNewTopicCollection() {
        mongoOriginalDatabase.createCollection(newTopicCollectionName);
        MongoCollection mongoCollection = mongoOriginalDatabase.getCollection(newTopicCollectionName);
        return mongoCollection;
    }

    public MongoDbTopicOperation(MongoClient mongoClient, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        mongoOriginalDatabase = mongoClient.getDatabase("joinUs");
        this.parallelExecutor = parallelExecutor;
    }

    public Document extractTopicDocument(Document oldDocument, String idField) {
        if (oldDocument == null || oldDocument.isEmpty()) return new Document();
        Document documentToReturn = new Document();
        List<String> keysToIncludeDirectly = List.of("description", "topic_name");

        documentToReturn.append(idField, oldDocument.getString("topic_id"));
        for (String key : oldDocument.keySet()) {
            if (keysToIncludeDirectly.contains(key))
                documentToReturn.append(key, oldDocument.getString(key));
        }

        return documentToReturn;
    }

    public void createTopicCollection() {
        MongoCollection newCollection = getNewTopicCollection();
        MongoCollection oldCollection = MongoDataLoader.csvDocuments.getCollection("topics.csv");
        try (MongoCursor<Document> mongoCursor = oldCollection.find().cursor()) {

            Document oldDocument;

            while (mongoCursor.hasNext()) {
                oldDocument = mongoCursor.next();

                Document finalOldDocument1 = oldDocument;

                Document finalOldDocument = oldDocument;
                parallelExecutor.submit(() -> newCollection.insertOne(extractTopicDocument(finalOldDocument, "_id")));
            }
        }
    }
}
