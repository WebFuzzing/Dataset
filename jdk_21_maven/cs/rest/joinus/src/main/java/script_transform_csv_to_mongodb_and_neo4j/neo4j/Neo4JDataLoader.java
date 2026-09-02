package script_transform_csv_to_mongodb_and_neo4j.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import script_transform_csv_to_mongodb_and_neo4j.ConfigurationFileReader;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.util.concurrent.Future;


public class Neo4JDataLoader {

    private static final String uri = ConfigurationFileReader.getNeo4JURL();
    private static final String dbUserName = ConfigurationFileReader.getNeo4JUsername();
    private static final String dbPassword = ConfigurationFileReader.getNeo4JPassword();
    private static final String defaultDatabase = ConfigurationFileReader.getNeo4JDatabase();

    private final ParallelExecutor parallelExecutor;

    public Neo4JDataLoader(ParallelExecutor parallelExecutor) {
        this.parallelExecutor = parallelExecutor;
    }

    public static Driver driver;

    private static void initDriver() {
        Config config = Config.builder()
                //                .withConnectionTimeout(2, TimeUnit.MINUTES)
                .withMaxConnectionPoolSize(10)
                .build();

        if (dbUserName != null && !dbUserName.isEmpty() && dbPassword != null && !dbPassword.isEmpty()) {
            driver = GraphDatabase.driver(uri, AuthTokens.basic(dbUserName, dbPassword), config);
        } else {
            driver = GraphDatabase.driver(uri, config);
        }
    }

    public static Driver getNeo4jDriver() {
        if (driver == null) {
            initDriver();
        }
        return driver;
    }

    public void transformCsvDataToNeo4j() throws Exception {
        Neo4JOperations neo4JOperations = new Neo4JOperations(getNeo4jDriver(), defaultDatabase, parallelExecutor);

        //        System.out.println("CREATING Nodes");
        Future[] futures = new Future[6];
        //
        futures[0] = parallelExecutor.submit(() -> {
            Neo4JOperations.createMemberNodes(null);
        });
        futures[1] = parallelExecutor.submit(() -> {
            Neo4JOperations.createEventNodes(null);
        });
        futures[2] = parallelExecutor.submit(() -> {
            Neo4JOperations.createGroupNodes(null);
        });
        futures[3] = parallelExecutor.submit(() -> {
            Neo4JOperations.createTopicNodes(null);
        });
        //
        ParallelExecutor.getFutures(futures);

        Neo4JOperations.createNeo4JIndex("Group", "group_id");
        Neo4JOperations.createNeo4JIndex("Member", "member_id");
        Neo4JOperations.createNeo4JIndex("Event", "event_id");
        Neo4JOperations.createNeo4JIndex("Event", "event_time");

        Neo4JOperations.createNeo4JIndex("Topic", "topic_id");

        System.out.println("CREATING Edges ");
        futures[2] = parallelExecutor.submit(() -> {
            Neo4JOperations.createGroupTopicsEdges();
        });
        futures[3] = parallelExecutor.submit(() -> {
            Neo4JOperations.createMemberEventsEdges();
        });

        futures[2].get();
        futures[3].get();
        System.out.println("FINISHED CREATING  Edges Group-Topics  and Member-Events ");

        futures[1] = parallelExecutor.submit(() -> {
            Neo4JOperations.createGroupEventsEdges();
        });
        futures[4] = parallelExecutor.submit(() -> {
            Neo4JOperations.createMemberTopicsEdges();
        });
        futures[1].get();
        futures[4].get();
        System.out.println("FINISHED CREATING  Edges Group-Events  and Member-Topics ");

        futures[5] = parallelExecutor.submit(() -> {
            Neo4JOperations.createMemberGroupsEdge();
        });
        System.out.println("FINISHED CREATING  Edges Group-Members ");

        ParallelExecutor.getFutures(futures);
        System.out.println("Finished CREATING Edges AND All the process for Neo4J");

        driver.close();

    }
}


















