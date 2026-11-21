package reciter.database.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbExtensionContext;
import software.amazon.awssdk.enhanced.dynamodb.extensions.ReadModification;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

public class LegacyS3AttributeExtension implements DynamoDbEnhancedClientExtension {

    @Override
    public ReadModification afterRead(DynamoDbExtensionContext.AfterRead context) {

        Map<String, AttributeValue> item =
                new HashMap<>(context.items());

        if (!item.containsKey("s3StorageFlag")
                && item.containsKey("usingS3")) {

            AttributeValue legacyValue = item.get("usingS3");

            if (legacyValue.bool() != null) {
                item.put(
                    "s3StorageFlag",
                    AttributeValue.builder()
                            .bool(legacyValue.bool())
                            .build()
                );

            } else if (legacyValue.n() != null) {
                item.put(
                    "s3StorageFlag",
                    AttributeValue.builder()
                            .bool("1".equals(legacyValue.n()))
                            .build()
                );
            }
        }

        return ReadModification.builder()
                .transformedItem(item)
                .build();
    }
}