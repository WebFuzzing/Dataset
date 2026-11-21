package it.pagopa.pn.mandate.middleware.db;

import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.middleware.db.entities.MandateSupportEntity;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.DeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;

import java.util.concurrent.ExecutionException;


@SpringBootTest
class TestDao extends BaseDao {

        DynamoDbAsyncTable<MandateEntity> mandateTable;
        DynamoDbAsyncTable<MandateSupportEntity> mandateSupportTable;
        DynamoDbAsyncTable<MandateEntity> mandateHistoryTable;

        public TestDao(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient, String table, String tableHistory)
        {
            this.mandateTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(MandateEntity.class));
            this.mandateSupportTable = dynamoDbEnhancedAsyncClient.table(table, TableSchema.fromBean(MandateSupportEntity.class));
            this.mandateHistoryTable = dynamoDbEnhancedAsyncClient.table(tableHistory, TableSchema.fromBean(MandateEntity.class));
        }

        public MandateEntity get(String pk, String sk) throws ExecutionException, InterruptedException {

            GetItemEnhancedRequest req = GetItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            return mandateTable.getItem(req).get();
        }

        public void delete(String pk, String sk) throws ExecutionException, InterruptedException {

            DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            mandateTable.deleteItem(req).get();
        }

        public MandateSupportEntity getSupport(String pk, String sk) throws ExecutionException, InterruptedException {

            GetItemEnhancedRequest req = GetItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            return mandateSupportTable.getItem(req).get();
        }

        public void deleteSupport(String pk, String sk) throws ExecutionException, InterruptedException {

            DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            mandateSupportTable.deleteItem(req).get();
        }

        public MandateEntity getHistory(String pk, String sk) throws ExecutionException, InterruptedException {

            GetItemEnhancedRequest req = GetItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            return mandateHistoryTable.getItem(req).get();
        }

        public void deleteHistory(String pk, String sk) throws ExecutionException, InterruptedException {

            DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                    .key(getKeyBuild(pk, sk))
                    .build();

            mandateHistoryTable.deleteItem(req).get();
        }

}