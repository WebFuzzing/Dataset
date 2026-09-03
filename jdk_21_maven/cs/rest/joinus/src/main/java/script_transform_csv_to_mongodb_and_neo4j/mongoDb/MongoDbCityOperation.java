package script_transform_csv_to_mongodb_and_neo4j.mongoDb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.util.List;


public class MongoDbCityOperation {

    public MongoClient mongoClient;
    public MongoDatabase mongoOriginalDatabase;
    public static String newCityCollectionName = "cities";
    private final ParallelExecutor parallelExecutor;

    public MongoDbCityOperation(MongoClient mongoClient, MongoDatabase mongoOriginalDatabase,
            ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        this.mongoOriginalDatabase = mongoOriginalDatabase;
        this.parallelExecutor = parallelExecutor;
    }

    public MongoCollection getCityCollection() {
        mongoOriginalDatabase.createCollection(newCityCollectionName);
        MongoCollection mongoCollection = mongoOriginalDatabase.getCollection(newCityCollectionName);
        return mongoCollection;
    }

    public MongoDbCityOperation(MongoClient mongoClient, ParallelExecutor parallelExecutor) {
        this.mongoClient = mongoClient;
        mongoOriginalDatabase = mongoClient.getDatabase("joinUs");
        this.parallelExecutor = parallelExecutor;
    }

    protected Document extractCityToEmbedFromId(String cityId) {
        MongoCollection cityCollection = MongoDataLoader.csvDocuments.getCollection("cities.csv");

        Document oldDocument = (Document) cityCollection.find(Filters.eq("city_id", cityId))
                .projection(new Document("_id", 0).append("member_count", 0).append("ranking", 0)).first();

        return extractCityDocument(oldDocument, "city_id");
    }

    protected static Document extractCityToEmbedFromCityName(String cityName) {
        MongoCollection cityCollection = MongoDataLoader.csvDocuments.getCollection("cities.csv");
        Document oldDocument = (Document) cityCollection.find(Filters.eq("city", cityName))
                .projection(new Document("_id", 0)).first();

        if (oldDocument == null) return new Document();

        Document newDocument = new Document();
        newDocument.append("name", oldDocument.get("city"));
        newDocument.append("city_id", oldDocument.get("city_id"));
        newDocument.append("state", oldDocument.get("state"));
        newDocument.append("country", oldDocument.get("country"));
        //        city_id
        //                name
        //        state
        //                country

        return newDocument;
    }

    private static Document extractCityDocument(Document oldDocument, String idField) {
        if (oldDocument == null || oldDocument.isEmpty()) return new Document();
        Document documentToReturn = new Document();
        List<String> keysToIncludeDirectly = List.of("country"
                , "state");
        for (String key : oldDocument.keySet()) {
            if (keysToIncludeDirectly.contains(key))
                documentToReturn.append(key, oldDocument.getString(key));
        }
        documentToReturn.append("name", oldDocument.get("city"));

        documentToReturn.append(idField, oldDocument.get("city_id"));

        double latitude = Double.parseDouble(oldDocument.getString("latitude"));
        double longitude = Double.parseDouble(oldDocument.getString("longitude"));

        documentToReturn.append("latitude", latitude);
        documentToReturn.append("longitude", longitude);

        return documentToReturn;
    }

    public void createCityCollection() {
        MongoCollection newCollection = getCityCollection();
        MongoCollection oldCollection = MongoDataLoader.csvDocuments.getCollection("cities.csv");

        try (MongoCursor<Document> mongoCursor = oldCollection.find().cursor()) {
            Document oldDocument;

            while (mongoCursor.hasNext()) {
                oldDocument = mongoCursor.next();
                Document finalOldDocument = oldDocument;
                parallelExecutor.submit(() -> {
                    newCollection.insertOne(extractCityDocument(finalOldDocument, "_id"));
                });
            }
        }
    }

}
