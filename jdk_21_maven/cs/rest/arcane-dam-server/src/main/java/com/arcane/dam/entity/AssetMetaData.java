package com.arcane.dam.entity;

// MODIFIED: migrated from AWS SDK v1 DynamoDB annotations to AWS SDK v2 Enhanced Client annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class AssetMetaData {
    private String id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private Instant uploadTimestamp;
    private String spaceId;
    private String userId;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("asset_id")
    public String getId() {
        return id;
    }

    @DynamoDbAttribute("file_name")
    public String getFileName() {
        return fileName;
    }

    @DynamoDbAttribute("file_type")
    public String getFileType() {
        return fileType;
    }

    @DynamoDbAttribute("file_size")
    public Long getFileSize() {
        return fileSize;
    }

    @DynamoDbAttribute("file_url")
    public String getFileUrl() {
        return fileUrl;
    }

    @DynamoDbAttribute("upload_timestamp")
    public Instant getUploadTimestamp() {
        return uploadTimestamp;
    }

    @DynamoDbAttribute("space_id")
    public String getSpaceId() {
        return spaceId;
    }

    @DynamoDbAttribute("user_id")
    public String getUserId() {
        return userId;
    }
}
