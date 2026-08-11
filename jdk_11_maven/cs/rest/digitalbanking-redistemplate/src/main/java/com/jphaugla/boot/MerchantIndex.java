package com.jphaugla.boot;

import com.redislabs.mesclun.search.*;
import com.redislabs.mesclun.StatefulRedisModulesConnection;
import com.redislabs.mesclun.RedisModulesCommands;

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
public class MerchantIndex implements CommandLineRunner {


  @Autowired
  private StatefulRedisModulesConnection<String,String> connection;

  @Value("${app.merchantSearchIndexName}")
  private String merchantSearchIndexName;

  @Override
  @SuppressWarnings({ "unchecked" })
  public void run(String... args) throws Exception {
    RedisModulesCommands<String,String> merchantCommands = connection.sync();
    try {
      merchantCommands.indexInfo(merchantSearchIndexName);
    } catch (RedisCommandExecutionException rcee) {
      if (rcee.getMessage().equals("Unknown Index name")) {

        CreateOptions<String, String> options = CreateOptions.<String, String>builder()//
            .prefix(merchantSearchIndexName + ':').build();

        Field merchantName = Field.text("name").build();
        Field categoryCode = Field.text("categoryCode").build();
        Field categoryDescription = Field.text("categoryDescription").build();
        Field merchantState = Field.text("state").build();
        Field merchantCountry = Field.text("countryCode").build();
        merchantCommands.create(
          merchantSearchIndexName, //
          options, //
                merchantName, categoryCode, categoryDescription, merchantState, merchantCountry
        );
        log.info(">>>> Created " + merchantSearchIndexName + " Search Index...");
      }
    }
  }

}
