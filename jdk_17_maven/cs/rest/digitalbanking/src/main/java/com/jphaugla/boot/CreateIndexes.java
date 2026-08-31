package com.jphaugla.boot;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

@Component
@Order(6)
@Slf4j
public class CreateIndexes implements CommandLineRunner {

  @Value("${app.accountSearchIndexName}")
  private String accountSearchIndexName;
  @Value("${app.customerSearchIndexName}")
  private String customerSearchIndexName;
  @Value("${app.emailSearchIndexName}")
  private String emailSearchIndexName;
  @Value("${app.merchantSearchIndexName}")
  private String merchantSearchIndexName;
  @Value("${app.phoneSearchIndexName}")
  private String phoneSearchIndexName;
  @Value("${app.transactionSearchIndexName}")
  private String transactionSearchIndexName;
  @Value("${app.transactionReturnSearchIndexName}")
  private String transactionReturnSearchIndexName;
  @Value("${app.disputeSearchIndexName}")
  private String disputeSearchIndexName;

  @Autowired
  private Environment env;

  UnifiedJedis client;
  IndexDefinition.Type indexType = IndexDefinition.Type.HASH;

  @Override
  @SuppressWarnings({ "unchecked" })
  public void run(String... args) throws Exception {

    client = jedis_connection();

    Schema accountIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("customerId").as("customerId"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("accountType").as("accountType"), Schema.FieldType.TAG))
        //    .addField(new Schema.Field(FieldName.of("accountOriginSystem").as("accountOriginSystem"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("accountStatus").as("accountStatus"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("cardNum").as("cardNum"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("openDate").as("openDate"), Schema.FieldType.NUMERIC))
         //   .addField(new Schema.Field(FieldName.of( "lastUpdated").as("lastUpdated"), Schema.FieldType.NUMERIC))
            ;
    tryIndex(client, accountSearchIndexName, accountIndexSchema);

    Schema customerIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("city").as("city"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("firstName").as("firstName"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("fullName").as("fullName"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("lastName").as("lastName"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("stateAbbreviation").as("stateAbbreviation"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("zipcode").as("zipcode"), Schema.FieldType.TAG))
         //   .addField(new Schema.Field(FieldName.of("customerId").as("customerId"), Schema.FieldType.TAG))
            ;
    tryIndex(client, customerSearchIndexName, customerIndexSchema);


    Schema emailIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("emailAddress").as("emailAddress"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("customerId").as("customerId"), Schema.FieldType.TAG))
            ;
    tryIndex(client, emailSearchIndexName, emailIndexSchema);

    Schema merchantIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("merchantName").as("merchantName"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("categoryCode").as("categoryCode"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("categoryDescription").as("categoryDescription"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("merchantState").as("merchantState"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("merchantCountry").as("merchantCountry"), Schema.FieldType.TAG))
            ;
    tryIndex(client, merchantSearchIndexName, merchantIndexSchema);

    Schema phoneIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("phoneNumber").as("phoneNumber"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("customerId").as("customerId"), Schema.FieldType.TAG))
            ;
    tryIndex(client, phoneSearchIndexName, phoneIndexSchema);

    Schema transactionIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("accountno").as("accountno"), Schema.FieldType.TAG))
       //     .addField(new Schema.Field(FieldName.of("amounttype").as("amounttype"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("merchant").as("merchant"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("status").as("status"), Schema.FieldType.TAG))
       //     .addField(new Schema.Field(FieldName.of("description").as("description"), Schema.FieldType.TAG))
       //     .addField(new Schema.Field(FieldName.of("referencekeytype").as("referencekeytype"), Schema.FieldType.TAG))
       //     .addField(new Schema.Field(FieldName.of("referencevalue").as("referencevalue"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("trancd").as("trancd"), Schema.FieldType.TAG))
        //    .addField(new Schema.Field(FieldName.of("location").as("location"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("transactionreturn").as("transactionreturn"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("initialdate").as("initialdate"), Schema.FieldType.NUMERIC))
            .addField(new Schema.Field(FieldName.of( "settlementdate").as("settlementdate"), Schema.FieldType.NUMERIC))
            .addField(new Schema.Field(FieldName.of( "postingdate").as("postingdate"), Schema.FieldType.NUMERIC))
            .addField(new Schema.Field(FieldName.of("transactiontags").as("transactiontags"), Schema.FieldType.TAG))
            ;
    tryIndex(client, transactionSearchIndexName, transactionIndexSchema);

    Schema transactionReturnIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("reasonCode").as("reasonCode"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("reasonDescription").as("reasonDescription"), Schema.FieldType.TAG))
            ;
    tryIndex(client, transactionReturnSearchIndexName, transactionReturnIndexSchema);
    Schema disputeIndexSchema = new Schema()
            .addField(new Schema.Field(FieldName.of("reasonCode").as("reasonCode"), Schema.FieldType.TAG))
            .addField(new Schema.Field(FieldName.of("reasonDescription").as("reasonDescription"), Schema.FieldType.TAG))
            ;
    tryIndex(client, disputeSearchIndexName, disputeIndexSchema);
  }
  public void tryIndex(UnifiedJedis jedis_client, String indexName, Schema schema) {
    log.info("rebuilding index on " + indexName);

    IndexDefinition indexDefinition = new IndexDefinition(indexType).setPrefixes(indexName + ':');
    try {
      jedis_client.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(indexDefinition), schema);
    } catch (Exception e) {
      jedis_client.ftDropIndex(indexName);
      jedis_client.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(indexDefinition), schema);
    }

  }
  private UnifiedJedis jedis_connection() {
    // Get the configuration from the application properties/environment
    UnifiedJedis unifiedJedis;
    String redisHost = "localhost"; // default name
    int redisPort = 6379;
    String redisPassword = "";

    redisHost = env.getProperty("redis.host", "localhost");
    redisPort = Integer.parseInt(env.getProperty("redis.port", "6379"));
    redisPassword = env.getProperty("spring.redis.password", "");

    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxIdle(50);
    poolConfig.setMaxTotal(50);
    HostAndPort hostAndPort = new HostAndPort(redisHost, redisPort);

    log.info( "Host: " + redisHost + " Port " + String.valueOf(redisPort));
    if (!(redisPassword.isEmpty())) {
      String redisURL = "redis://:" + redisPassword + '@' + redisHost + ':' + String.valueOf(redisPort);
      log.info("redisURL is " + redisURL);
      unifiedJedis = new JedisPooled(redisURL);
    } else {
      log.info(" no password");
      unifiedJedis = new JedisPooled(hostAndPort);
    }
    return unifiedJedis;
  }

}
