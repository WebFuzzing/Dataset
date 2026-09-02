package script_transform_csv_to_mongodb_and_neo4j.neo4j;

import com.opencsv.CSVReader;
import org.bson.Document;
import org.neo4j.driver.*;
import script_transform_csv_to_mongodb_and_neo4j.ConfigurationFileReader;
import script_transform_csv_to_mongodb_and_neo4j.CsvDataOperations;
import script_transform_csv_to_mongodb_and_neo4j.ParallelExecutor;

import java.io.FileReader;
import java.util.*;


public class Neo4JOperations {

    public static Driver driver = Neo4JDataLoader.getNeo4jDriver();
    public static String neo4jDatabase = ConfigurationFileReader.getNeo4JDatabase();
    public static String transferDataToNeo4J = ConfigurationFileReader.checkAndGetProp("transferDataToNeo4J");
    private static final String ImportFolder = "file:///";
    private static final String BATCH_SIZE = ConfigurationFileReader.checkAndGetProp("importBatchSize");

    private static final List<String> groupProperties = List.of("group_id", "group_name", "city", "description",
            "category_name");
    private static final List<String> eventProperties = List.of("event_id", "event_name", "event_time", "description",
            "fee_amount", "venue_city", "group_name", "group_id", "venue_address_1");
    private static final List<String> topicProperties = List.of("topic_id", "topic_name");
    private static final List<String> memberProperties = List.of("member_id", "city", "member_name");

    public Neo4JOperations(Driver driver, String neo4jDatabase, ParallelExecutor parallelExecutor) {
        Neo4JOperations.driver = driver;
        Neo4JOperations.neo4jDatabase = neo4jDatabase;
    }

    private static void createNode(String fileName, String nodeName, List<String> columnsToInclude) {
        StringJoiner columnText = new StringJoiner(",");

        String column = null;

        for (String str : columnsToInclude) {
            //            column = str.replace("___", "_");
            column = str;

            if (column.equalsIgnoreCase("event_time")) {
                columnText.add(
                        "event_time: datetime(replace(row.event_time,' ','T'))" +
                                " + duration({days: 3287})"
                );
            } else {
                columnText.add(column + ": row." + column);
            }
        }
        String txt = columnText.toString();
        //            .substring(0,columnText.length()-1);
        try (Session session = driver.session(SessionConfig.builder().withDatabase(neo4jDatabase).build())) {
            session.run("   LOAD CSV WITH HEADERS FROM '" + ImportFolder + fileName + "' AS row " +
                    "CALL {" +
                    "WITH row "
                    + "   MERGE (m:" + nodeName + " { " + txt + " })" +
                    " } IN TRANSACTIONS OF " + BATCH_SIZE + " ROWS;");

        }
    }

    public static void createGroupNodes(List<String> fieldsToInclude) {
        List<String> fields;
        if (fieldsToInclude == null || fieldsToInclude.isEmpty()) {
            fields = groupProperties;
        } else fields = fieldsToInclude;
        createNode("groups.csv", "Group", fields);

    }

    public static void createEventNodes(List<String> fieldsToInclude) {
        List<String> fields;
        if (fieldsToInclude == null || fieldsToInclude.isEmpty()) {
            fields = eventProperties;
        } else fields = fieldsToInclude;
        createNode("events.csv", "Event", fields);
    }

    public static void createTopicNodes(List<String> fieldsToInclude) {
        List<String> fields;
        if (fieldsToInclude == null || fieldsToInclude.isEmpty()) {
            fields = topicProperties;
        } else fields = fieldsToInclude;
        createNode("topics.csv", "Topic", fields);
    }

    public static void createMemberNodes(List<String> fieldsToInclude) {

        //        createNode("members.csv","Member",memberProperties);

        String path = CsvDataOperations.transformedMembers;
        try {
            CSVReader csvReader = new CSVReader(new FileReader(path));

            String[] header = csvReader.readNext();
            int memberIdIndex = -1;
            int cityIndex = -1;
            int memberNameIndex = -1;

            for (int i = 0; i < header.length; i++) {
                if (header[i].equalsIgnoreCase("member_id")) memberIdIndex = i;
                else if (header[i].equalsIgnoreCase("city")) {
                    cityIndex = i;
                } else if (header[i].equalsIgnoreCase("member_name")) {
                    memberNameIndex = i;
                }
            }

            if (memberNameIndex == -1 || cityIndex == -1 || memberIdIndex == -1)
                throw new Exception("File does not have the column");

            String[] row;
            Set<String> membersIncluded = new HashSet<>();
            while ((row = csvReader.readNext()) != null) {

                String member_id = row[memberIdIndex];

                if (!membersIncluded.contains(member_id)) {
                    String member_name = row[memberNameIndex];
                    String city = row[cityIndex];

                    String query = """
                                CREATE (m:Member {
                                    member_id: $memberId,
                                    city: $city,
                                    member_name: $memberName
                                })
                            """;

                    driver.executableQuery(query)
                            .withParameters(Map.of(
                                    "memberId", member_id,
                                    "city", city,
                                    "memberName", member_name
                            ))
                            .withConfig(QueryConfig.builder()
                                    .withDatabase(neo4jDatabase)
                                    .build())
                            .execute();
                    membersIncluded.add(member_id);
                }

            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static void createNeo4JIndex(String nodeName, String indexKey) {

        executeQueryInParallel(driver.executableQuery(" CREATE INDEX " + indexKey + "_index"
                + " IF NOT EXISTS "
                + " FOR (e:" + nodeName + ")"
                + "  ON (e." + nodeName + ");").withConfig(QueryConfig.builder()
                .withDatabase(neo4jDatabase).build()));
    }

    /**
     * Function to remove the quotas from the keys
     */
    private static String docToNeo4jNode(Document document) {
        StringBuilder node = new StringBuilder("{");
        for (String key : document.keySet()) {
            node.append(key.replaceAll("\"", ""));
            node.append(": ");
            node.append("\"" + (document.get(key) != null ? document.get(key).toString() : "") + "\"");
            node.append(",");
        }
        node.deleteCharAt(node.lastIndexOf(","));
        node.append("}");
        return node.toString();

    }

    private static void executeQueryInParallel(ExecutableQuery executableQuery) {
        //    parallelExecutor.submit(() -> executableQuery.execute());
        if (transferDataToNeo4J.equalsIgnoreCase("false")) {}
        else executableQuery.execute();
    }

    protected static void createMemberGroupsEdge() {
        addEdges("members.csv", "member_id", "group_id");
        addEdges("rsvps.csv", "member_id", "group_id");
    }

    protected static void createGroupTopicsEdges() {
        addEdges("groups_topics.csv", "group_id", "topic_id");
    }

    protected static void createMemberTopicsEdges() {
        addEdges("members_topics.csv", "member_id", "topic_id");
    }

    protected static void createGroupEventsEdges() {
        addEdges("rsvps.csv", "group_id", "event_id");
    }

    protected static void createMemberEventsEdges() {
        addEdges("rsvps.csv", "member_id", "event_id");
    }

    //    private static void createEdgesFromCSV(String filePath,String firstKey,String secondKey){
    //   double BATCH_SIZE = 1_000;
    //    try (
    //            Session session = driver.session();
    //            CSVReader reader = new CSVReader(new FileReader(filePath));
    //    ) {
    //        List<Map<String, Object>> batch = new ArrayList<>();
    //        List<String[]> batch2=new ArrayList<>();
    //        String[] row;
    //
    //        String[] header = reader.readNext();
    //        int indexOf_firstKey=-1;
    //        int index_Of_secondKey=-1;
    //        for (int i=0;i<header.length;i++){
    //            if (header[i].equalsIgnoreCase(firstKey)) indexOf_firstKey=i;
    //            else if (header[i].equalsIgnoreCase(secondKey)) index_Of_secondKey=i;
    //        }
    //        if (indexOf_firstKey==-1 || index_Of_secondKey==-1) throw new RuntimeException("The key does not exist");
    //
    //        while ((row = reader.readNext()) != null) {
    //
    //
    //            addEdge(firstKey,secondKey,new String[]{row[indexOf_firstKey],row[index_Of_secondKey]});
    //
    ////            batch2.add(new String[]{row[indexOf_firstKey],row[index_Of_secondKey]});
    ////
    ////            if (batch2.size() == BATCH_SIZE) {
    ////                writeBatch( firstKey, secondKey,batch2);
    ////                batch2.clear();
    ////            }
    //        }

    /// / /        if (!batch2.isEmpty()) { /            writeBatch( firstKey, secondKey, batch2); /        }
    //    } catch (FileNotFoundException e) {
    //        throw new RuntimeException(e);
    //    } catch (IOException | CsvValidationException e) {
    //        throw new RuntimeException(e);
    //    }
    //
    //
    //}
    //    private static void addEdge(String firstKey,String secondKey, String[] row) {
    //        String firstNode=findNode(firstKey);
    //        String secondNode=findNode(secondKey);
    //        String relationship = findRelationship(firstKey,secondKey);
    //        executeQueryInParallel(driver.executableQuery(
    //
    //                " MATCH (m:"+firstNode+" { "+firstKey+": \""+row[0]+"\" }) "
    //                +" MATCH (e:"+secondNode+"  { "+secondKey+": \""+row[1]+"\" }) "
    //                +" MERGE (m)-[:"+relationship+"]->(e) "
    //                +" MERGE (m)<-[:"+relationship+"]-(e) "
    //                + "").withConfig(QueryConfig.builder()
    //                .withDatabase(neo4jDatabase).build()));
    //    }
    //
    //    private static void writeBatch(String firstKey,String secondKey, List<String[]> batch) {
    //
    //        String firstNode=findNode(firstKey);
    //        String secondNode=findNode(secondKey);
    //        String relationship = findRelationship(firstKey,secondKey);
    //
    //        try (Session session = driver.session()) {
    //            session.executeWrite(tx ->
    //                    tx.run(
    //                            ""
    //                           + " UNWIND $rows AS row "
    //                           +" MATCH (m:"+firstNode+" {"+firstKey+": row[0]}) "
    //                           +" MATCH (e:"+secondNode+"  {"+secondKey+": row[1]}) "
    //                           +" MERGE (m)-[:"+relationship+"]->(e) "
    //                            +" MERGE (m)<-[:"+relationship+"]-(e) "
    //                                    + "",
    //                            Map.of("rows", batch)
    //                    )
    //            );
    //        }
    //    }
    private static String findRelationship(String firstKey, String secondKey) {
        if ((firstKey.equalsIgnoreCase("member_id")
                && secondKey.equalsIgnoreCase("group_id")) ||
                (firstKey.equalsIgnoreCase("group_id")
                        && secondKey.equalsIgnoreCase("member_id"))) return "MEMBER_OF";
        else if ((firstKey.equalsIgnoreCase("topic_id")
                && secondKey.equalsIgnoreCase("group_id")) ||
                (firstKey.equalsIgnoreCase("group_id")
                        && secondKey.equalsIgnoreCase("topic_id"))) return "HAS_TOPIC";
        else if ((firstKey.equalsIgnoreCase("event_id")
                && secondKey.equalsIgnoreCase("group_id")) ||
                (firstKey.equalsIgnoreCase("group_id")
                        && secondKey.equalsIgnoreCase("event_id"))) return "ORGANIZES";
        else if ((firstKey.equalsIgnoreCase("event_id")
                && secondKey.equalsIgnoreCase("member_id")) ||
                (firstKey.equalsIgnoreCase("member_id")
                        && secondKey.equalsIgnoreCase("event_id"))) return "ATTENDS";
        else if ((firstKey.equalsIgnoreCase("topic_id")
                && secondKey.equalsIgnoreCase("member_id")) ||
                (firstKey.equalsIgnoreCase("member_id")
                        && secondKey.equalsIgnoreCase("topic_id"))) return "INTERESTED_IN";
        return null;
    }

    private static String findNode(String primaryKey) {
        if (primaryKey.equalsIgnoreCase("member_id")) return "Member";
        if (primaryKey.equalsIgnoreCase("group_id")) return "Group";
        if (primaryKey.equalsIgnoreCase("event_id")) return "Event";
        if (primaryKey.equalsIgnoreCase("topic_id")) return "Topic";

        return null;
    }

    //    private static void createNodeFromDocument(String nodesName,Document documentToModify,List<String> fieldsToInclude,boolean flattenTheDocument){
    //        Document document = new Document();
    //        for (String key:documentToModify.keySet()){
    //            if (fieldsToInclude.contains(key)){
    //                document.append(key,documentToModify.get(key));
    //            }
    //        }
    //        if (flattenTheDocument) document = MongoDataLoader.flattenDocument(document);
    //        String queryForNeo4j = "CREATE (p:" + nodesName + "  " +docToNeo4jNode(document) + " )";
    //        executeQueryInParallel(
    //                driver.executableQuery(queryForNeo4j).withConfig(QueryConfig.builder().withDatabase(neo4jDatabase).build())

    /// /                            .execute();
    //        );
    //    }
    private static void addEdges(String fileName, String firstKey, String secondKey) {
        String firstNode = findNode(firstKey);
        String secondNode = findNode(secondKey);
        String relationship = findRelationship(firstKey, secondKey);

        //        String str1="" +
        //                "   LOAD CSV WITH HEADERS FROM '"+ ImportFolder +fileName+"' AS row " +
        //                "CALL {" +
        //                "WITH row "
        //                +" MATCH (m:"+firstNode+" { "+firstKey+": row."+firstKey+" }) " +
        //                "    MATCH (e:"+secondNode+" { "+secondKey+": row."+secondKey+" })  " +
        //                " CREATE (m)-[:"+relationship+"]->(e)  ";

        try (Session session = driver.session(
                SessionConfig.builder().withDatabase(ConfigurationFileReader.getNeo4JDatabase()).build())) {

            //        + "  SET m.name = row.name"+
            session.run("   LOAD CSV WITH HEADERS FROM '" + ImportFolder + fileName + "' AS row " +
                    "CALL {" +
                    "WITH row "
                    + " MATCH (m:" + firstNode + " { " + firstKey + ": row." + firstKey + " }) " +
                    "    MATCH (e:" + secondNode + " { " + secondKey + ": row." + secondKey + " })  " +
                    " CREATE (m)-[:" + relationship + "]->(e)  " +
                    " } IN TRANSACTIONS OF " + BATCH_SIZE + " ROWS;");
/*
+" MATCH (m:"+firstNode+" {"+firstKey+": row[0]}) "
                           +" MATCH (e:"+secondNode+"  {"+secondKey+": row[1]}) "
                           +" MERGE (m)-[:"+relationship+"]->(e) "
                            +" MERGE (m)<-[:"+relationship+"]-(e) "
 */
        }

    }

}
