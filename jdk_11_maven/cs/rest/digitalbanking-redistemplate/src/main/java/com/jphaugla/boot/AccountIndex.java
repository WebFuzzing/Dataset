package com.jphaugla.boot;

import com.redislabs.mesclun.RedisModulesCommands;
import com.redislabs.mesclun.StatefulRedisModulesConnection;
import com.redislabs.mesclun.search.CreateOptions;
import com.redislabs.mesclun.search.Field;
import io.lettuce.core.RedisCommandExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(6)
@Slf4j
public class AccountIndex implements CommandLineRunner {

  @Autowired
  private StatefulRedisModulesConnection<String,String> connection;

  @Value("${app.accountSearchIndexName}")
  private String accountSearchIndexName;

  @Override
  @SuppressWarnings({ "unchecked" })
  public void run(String... args) throws Exception {
    RedisModulesCommands<String,String> accountCommands = connection.sync();
    try {
      accountCommands.indexInfo(accountSearchIndexName);
    } catch (RedisCommandExecutionException rcee) {
      if (rcee.getMessage().equals("Unknown Index name")) {

        CreateOptions<String, String> options = CreateOptions.<String, String>builder()//
            .prefix(accountSearchIndexName + ':').build();

        Field customerId = Field.text("customerId").build();
        Field accountType = Field.text("accountType").build();
        Field accountOriginSystem = Field.text("accountOriginSystem").build();
        Field accountStatus = Field.text("accountStatus").build();
        Field cardNum = Field.text("cardNum").build();
        Field openDate = Field.numeric("openDate").sortable(true).build();
        Field lastUpdated = Field.numeric("lastUpdated").sortable(true).build();
         accountCommands.create(
          accountSearchIndexName, //
          options, //
                customerId, accountType, accountOriginSystem, accountStatus, cardNum, openDate, lastUpdated
        );
        log.info(">>>> Created " + accountSearchIndexName + " Search Index...");
      }
    }
  }

}
