package reciter.database.dynamodb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import reciter.engine.analysis.ReCiterFeature;
import reciter.model.identity.Identity;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * This class allows you to store dynamodb items which exceeds dynamodb item limit of 400kb in s3.
 * Pre-requisite s3 properties should be set in application properties
 * @author Sarbajit Dutta(szd2013)
 *
 */
@Component
public class DynamoDbS3Operations {
	
	private static final Logger log = LoggerFactory.getLogger(DynamoDbS3Operations.class);
	
	@Lazy
	@Autowired
	private S3Client  s3;
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule());
	
	private static final String CONTENT_TYPE = "application/json";
	
	/**
	 * This function stores large object which has size more than 400kb.
	 * @param bucketName
	 * @param object
	 * @param keyName
	 */
	public boolean saveLargeItem(String bucketName, Object object, String keyName) {
		// Skip S3 entirely when client or bucket is unavailable (e.g., standalone
		// local mode with aws.s3.use=false).
		if (s3 == null || bucketName == null) {
			return false;
		}

		byte[] objectContentBytes;
		try {
			objectContentBytes = OBJECT_MAPPER.writeValueAsString(object).getBytes(StandardCharsets.UTF_8);
		} catch (JsonProcessingException e) {
			log.error("S3 offload failed to serialize key={}: {}", keyName, e.getMessage(), e);
			return false;
		}

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName.toLowerCase())
				.key(keyName)
				.contentType(CONTENT_TYPE)
				.contentLength((long) objectContentBytes.length)
				.build();

		try {
			// putObject overwrites an existing key in place. The previous code deleted the
			// object first and only then re-uploaded, so a failed re-upload destroyed the
			// payload while the DynamoDB row still claimed the record was S3-backed.
			s3.putObject(putObjectRequest,
					RequestBody.fromInputStream(new ByteArrayInputStream(objectContentBytes), objectContentBytes.length));
			return true;
		} catch (SdkException e) {
			log.error("S3 putObject failed for bucket={} key={}: {}", bucketName, keyName, e.getMessage(), e);
			return false;
		}
	}
	
	private boolean s3ObjectExists(String bucketName, String keyName) {
	    try {
	        s3.headObject(HeadObjectRequest.builder()
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

	
	/**
	 * This function retrieves large object from S3
	 * @param bucketName
	 * @param keyName
	 * @param objectClass
	 * @return
	 */
	public <T> Object retrieveLargeItem(String bucketName, String keyName, Class<T> objectClass) {
		// Match the null guard in saveLargeItem. Return null so callers fall back
		// to the standard DynamoDB read path (which the caller already does for
		// items <= 400KB).
		if (s3 == null || bucketName == null) {
			return null;
		}
		log.info("retrieveLargeItem from S3: key={}, bucket={}, objectClass={}", keyName, bucketName, objectClass);
		try {
			//S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName.toLowerCase(), keyName));
			
			GetObjectRequest getObjectRequest  = GetObjectRequest.builder()
			        .bucket(bucketName.toLowerCase())
			        .key(keyName)
			        .build();
			try (ResponseInputStream<GetObjectResponse> s3Object = s3.getObject(getObjectRequest)) {
			    String objectContent = IOUtils.toString(s3Object, StandardCharsets.UTF_8);

				 if (objectClass == ReCiterFeature.class) {
			        return OBJECT_MAPPER.readValue(objectContent, ReCiterFeature.class);
			    }
			    // Identity is stored as an array and returned as a List. Every other
			    // offloaded type (ReCiterFeature, PubMedArticle, ...) deserializes
			    // directly to objectClass. The previous code only had explicit
			    // branches for ReCiterFeature and Identity, so any other type — most
			    // importantly PubMedArticle — fell through to `return null`, silently
			    // dropping the offloaded article and NPE-ing the caller.
			    if (objectClass == Identity.class) {
			        return Arrays.asList(OBJECT_MAPPER.readValue(objectContent, Identity[].class));
			    }
			    //return OBJECT_MAPPER.readValue(objectContent, objectClass);
			}catch (NoSuchKeyException e) {
    log.error("S3 object not found. bucket={}, key={}", bucketName, keyName, e);
    
} catch (S3Exception e) {
    log.error(
        "AWS S3 error. bucket={}, key={}, statusCode={}, errorCode={}",
        bucketName,
        keyName,
        e.statusCode(),
        e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "unknown",
        e
    );

} catch (JsonMappingException e) {
    log.error(
        "JSON mapping failed. bucket={}, key={}, targetClass={}",
        bucketName,
        keyName,
        objectClass.getName(),
        e
    );

} catch (JsonProcessingException e) {
    log.error(
        "Invalid JSON content. bucket={}, key={}, targetClass={}",
        bucketName,
        keyName,
        objectClass.getName(),
        e
    );

} catch (IOException e) {
    log.error(
        "IO error reading S3 object. bucket={}, key={}",
        bucketName,
        keyName,
        e
    );

} 
			/*catch (IOException | S3Exception e) {
			    return OBJECT_MAPPER.readValue(objectContent, objectClass);
			} catch (IOException | S3Exception e) {
			    log.error("Error retrieving object from S3: {}", e.getMessage());
			}*/
		

		
	}
		catch (SdkServiceException e) {
			log.error("S3 getObject failed for bucket={} key={}: {}", bucketName, keyName, e.getMessage(), e);
		}
		return null;
	}
	
	/**
	 * This function delete large objects from S3
	 * @param bucketName
	 * @param keyName
	 */
	/*public void deleteLargeItem(String bucketName, String keyName) {
		if(s3 != null && bucketName != null && s3.doesObjectExist(bucketName.toLowerCase(), keyName)) {
			log.info("Deleting Object from bucket " + bucketName + " with keyName " + keyName);
			s3.deleteObject(bucketName.toLowerCase(), keyName);
		}
	}
	*/
	public void deleteLargeItem(String bucketName, String keyName) {
	    if (s3 != null && bucketName != null && s3ObjectExists(bucketName.toLowerCase(), keyName)) {
	        log.info("Deleting Object from bucket " + bucketName + " with keyName " + keyName);

	        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
	                .bucket(bucketName.toLowerCase())
	                .key(keyName)
	                .build();

	        s3.deleteObject(deleteObjectRequest);
	    }
	}


	/**
	 * This function gets the timestamp of the object that was stored. It assumes versioning is turned off for bucket.
	 * @param bucketName
	 * @param keyName
	 * @return date of the object that was stored
	 */
	public Date getObjectSaveTimestamp(String bucketName, String keyName) {
		// Match the null guards in saveLargeItem / retrieveLargeItem. Return null
		// when the S3 client or bucket is unavailable so the caller falls back to
		// a fresh DynamoDB read instead of throwing NullPointerException.
		if (s3 == null || bucketName == null) {
			return null;
		}
	    try {
	        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
	                .bucket(bucketName.toLowerCase())
	                .key(keyName)
	                .build();

	        HeadObjectResponse headObjectResponse = s3.headObject(headObjectRequest);

	        return Date.from(headObjectResponse.lastModified());
	    } catch (S3Exception e) {
	        log.error("Error retrieving object metadata from S3: {}", e.awsErrorDetails().errorMessage());
	    }
	    return null;
	}

}
