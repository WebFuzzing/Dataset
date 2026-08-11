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
public class PhoneIndex implements CommandLineRunner {

  @Autowired
  private StatefulRedisModulesConnection<String,String> connection;

  @Value("${app.phoneSearchIndexName}")
  private String phoneSearchIndexName;

  @Override
  @SuppressWarnings({ "unchecked" })
  public void run(String... args) throws Exception {
    RedisModulesCommands<String,String> phoneCommands = connection.sync();
    try {
      phoneCommands.indexInfo(phoneSearchIndexName);
    } catch (RedisCommandExecutionException rcee) {
      if (rcee.getMessage().equals("Unknown Index name")) {

        CreateOptions<String, String> options = CreateOptions.<String, String>builder()//
            .prefix(phoneSearchIndexName + ':').build();

        Field phoneNumber = Field.text("phoneNumber").build();
        Field customerId = Field.text("customerId").build();
         phoneCommands.create(
          phoneSearchIndexName, //
          options, //
                phoneNumber, customerId
        );
        log.info(">>>> Created " + phoneSearchIndexName + " Search Index...");
      }
    }
  }

}
