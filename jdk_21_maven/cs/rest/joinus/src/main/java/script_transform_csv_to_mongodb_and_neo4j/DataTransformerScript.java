package script_transform_csv_to_mongodb_and_neo4j;

import script_transform_csv_to_mongodb_and_neo4j.mongoDb.MongoDataLoader;
import script_transform_csv_to_mongodb_and_neo4j.neo4j.Neo4JDataLoader;
import script_transform_csv_to_mongodb_and_neo4j.neo4j.Neo4JOperations;

import java.util.Date;
import java.util.concurrent.Future;


public class DataTransformerScript {

    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis();
        System.out.println("STARTING TIME " + new Date());
        ParallelExecutor parallelExecutor = new ParallelExecutor();

        CsvDataOperations.updateIdsDirectlyFromCSV();

        Future future1 = parallelExecutor.submit(() -> {
            try {
                new MongoDataLoader(parallelExecutor).transformCsvDataToMongoDB();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Future future2 = null;
        if (Neo4JOperations.transferDataToNeo4J.equalsIgnoreCase("true")) {
            CsvDataOperations.copyFilesToNeo4JImportFolder();
            future2 = parallelExecutor.submit(() -> {
                try {
                    new Neo4JDataLoader(parallelExecutor).transformCsvDataToNeo4j();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        future2.get();
        future1.get();

        parallelExecutor.close();
        long endTime = System.currentTimeMillis();
        System.out.println("THE PROCESS TOOK " + (endTime - startTime) / 1000 + " seconds");
    }
}
