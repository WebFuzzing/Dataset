package com.jphaugla.boot;

import com.redislabs.mesclun.search.*;
import com.redislabs.mesclun.StatefulRedisModulesConnection;
import com.redislabs.mesclun.RedisModulesCommands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.lettuce.core.RedisCommandExecutionException;


import lombok.extern.slf4j.Slf4j;

@Component
@Order(6)
@Slf4j
public class TransactionIndex implements CommandLineRunner {

  @Autowired
  private StatefulRedisModulesConnection<String,String> connection;

  @Value("${app.transactionSearchIndexName}")
  private String transactionSearchIndexName;


  @Override
  @SuppressWarnings({ "unchecked" })
  public void run(String... args) throws Exception {

    RedisModulesCommands<String,String> transactionCommands = connection.sync();

    try {
      transactionCommands.indexInfo(transactionSearchIndexName);
    } catch (RedisCommandExecutionException rcee) {
      if (rcee.getMessage().equals("Unknown Index name")) {

        CreateOptions<String, String> options = CreateOptions.<String, String>builder()//
            .prefix(transactionSearchIndexName + ':').build();

        Field accountNo = Field.text("accountNo").build();
        Field amountType = Field.text("amountType").build();
        Field merchant = Field.text("merchant").build();
        Field status = Field.text("status").sortable(true).build();
        Field description = Field.text("description").build();
        Field referenceKeyType = Field.text("referenceKeyType").build();
        Field referenceValue = Field.text("referenceValue").build();
        Field tranCd = Field.text("tranCd").build();
        Field location = Field.text("location").build();
        Field transactionReturn = Field.text("transactionReturn").build();
        Field initialDate = Field.numeric("initialDate").sortable(true).build();
        Field settlementDate = Field.numeric("settlementDate").sortable(true).build();
        Field postingDate = Field.numeric("postingDate").sortable(true).build();
        Field transactionTags = Field.tag("transactionTags").separator(":").sortable(true).build();

         transactionCommands.create(
          transactionSearchIndexName, //
          options, //
                 accountNo, amountType, merchant, status, description, referenceKeyType, tranCd, location, transactionReturn, referenceValue, initialDate, settlementDate, postingDate,transactionTags
        );

        log.info(">>>> Created " + transactionSearchIndexName + " Search Index...");
      }
    }
  }

}
