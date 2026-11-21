package com.arcane.dam.config.aws;

// MODIFIED: migrated from DynamoDBMapper (SDK v1) to DynamoDbEnhancedClient (SDK v2)
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Configuration
@RequiredArgsConstructor
public class DynamoDBConfig {

    private final AWSConfig awsConfig;

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(awsConfig.buildAmazonDynamoDB())
                .build();
    }
}
