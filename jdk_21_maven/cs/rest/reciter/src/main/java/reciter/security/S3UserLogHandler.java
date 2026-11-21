package reciter.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3UserLogHandler {

    private static final Logger log = LoggerFactory.getLogger(S3UserLogHandler.class);
    private S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.consumer.api.logs.bucketName}")
    private String apiLogsBucketName;
    
    @Value("${aws.cognito.userpool.region}")
    private String apiLogsBucketRegion;
    
    public S3UserLogHandler() {
 		this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        if (apiLogsBucketRegion != null && !apiLogsBucketRegion.isBlank()) {
			s3Client = S3Client.builder().credentialsProvider(DefaultCredentialsProvider.create())
					.region(Region.of(apiLogsBucketRegion)).build();
        } else {
            throw new IllegalStateException("AWS region is not configured correctly");
        }
    }

    // Path format: transactions/YYYY-MM-DD/logs.json
    private String getLogFilePath(String date) {
    	return String.format("%s.json", date);
    }

    // Method to create or append user log entry to the log file
	public void writeUserLog(UserLog userLog, String date) throws IOException {
		String logFilePath = getLogFilePath(date);
		List<UserLog> logs = new ArrayList<>();
		try {

			boolean objectExists = s3ObjectExists(apiLogsBucketName, logFilePath);

			if (objectExists) {

				log.info("Reading existing log file from S3. bucketName={}, key={}", apiLogsBucketName, logFilePath);

				GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(apiLogsBucketName)
						.key(logFilePath).build();

				try (ResponseInputStream<?> inputStream = s3Client.getObject(getObjectRequest)) {

					UserLog[] existingLogs = objectMapper.readValue((InputStream) inputStream, UserLog[].class);

					logs.addAll(List.of(existingLogs));
				}
			}

			logs.add(userLog);

			String jsonLogs = objectMapper.writeValueAsString(logs);

			PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(apiLogsBucketName).key(logFilePath)
					.contentType("application/json").build();

			s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonLogs));

			log.info("Successfully wrote user log to S3. bucketName={}, key={}, totalLogs={}", apiLogsBucketName,
					logFilePath, logs.size());

		} catch (S3Exception e) {

			log.error("S3 exception while writing user log. bucketName={}, key={}", apiLogsBucketName, logFilePath, e);

			throw e;

		} catch (Exception e) {

			log.error("Unexpected exception while writing user log. bucketName={}, key={}", apiLogsBucketName,
					logFilePath, e);

			throw new IOException("Failed to write user log to S3", e);
		}
	}
    private boolean s3ObjectExists(String bucketName, String keyName) {
	    try {
	    	s3Client.headObject(HeadObjectRequest.builder()
	                .bucket(bucketName)
	                .key(keyName)
	                .build());
	        return true; // Object exists
	    } catch (NoSuchKeyException e) {
	        return false; // Object does not exist
	    } catch (S3Exception e) {
	        log.error("Error checking object existence in S3: {}", e.awsErrorDetails().errorMessage());
	        return false;
	    }
	}
}


