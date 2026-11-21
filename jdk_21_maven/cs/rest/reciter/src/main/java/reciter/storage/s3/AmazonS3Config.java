package reciter.storage.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@Configuration
@ComponentScan(basePackages ="reciter.storage.s3")
public class AmazonS3Config {
	
	private static final Logger log = LoggerFactory.getLogger(AmazonS3Config.class);
	
	private String amazonAWSAccessKey = System.getenv("AMAZON_AWS_ACCESS_KEY");

    private String amazonAWSSecretKey = System.getenv("AMAZON_AWS_SECRET_KEY");
    
    @Value("${aws.s3.use}")
    private boolean isS3Use;
    
    @Value("${aws.s3.region}")
    private String awsS3Region;
    
    @Value("${aws.s3.dynamodb.bucketName}")
    private String s3BucketName;
    
    @Value("${aws.dynamoDb.local}")
    private boolean isDynamoDbLocal;
    
    @Value("${aws.s3.use.dynamic.bucketName}")
	private boolean isDynamicBucketName;
	
	/**
	 * This static variable will hold the s3 bucketName based on dynamic bucket generation
	 */
	public static String BUCKET_NAME;
    
    /**
     * @return S3Client client object
     */
    @Bean
    @Scope("singleton")
    public S3Client amazonS3() {
    	
    	if(isS3Use && !isDynamoDbLocal) {
    		final S3Client s3 = S3Client.builder()
    			    .credentialsProvider(DefaultCredentialsProvider.create())
    			    .region(Region.of(System.getenv("AWS_REGION"))) 
    			    .build();
	    	
	    	createBucket(s3);
	    	return s3;
    	}
    						
    	return null;		
    }
    
    private void createBucket(S3Client s3) {
    	String accountNumber = getAccountIDUsingAccessKey(amazonAWSAccessKey, amazonAWSSecretKey);
    	BUCKET_NAME = s3BucketName.toLowerCase() + "-" + System.getenv("AWS_REGION").toLowerCase() + "-" + accountNumber;
    	if(!isDynamicBucketName) {
    		BUCKET_NAME = s3BucketName;
		}
    	try {
            // Check if the bucket exists
            s3.headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
            log.info(BUCKET_NAME.toLowerCase() + " Bucket Name already exists");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                // Bucket does not exist, create it
                try {
                    s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME.toLowerCase()).build());
                    log.info("Bucket created with name: " + BUCKET_NAME);
                } catch (S3Exception createEx) {
                    log.error("Error creating bucket: " + createEx.getMessage());
                }
            } else {
                log.error("Error checking bucket existence: " + e.getMessage());
            }
        }
    }
    
    /**
     * This function creates empty folder in a s3 bucket
     * @param bucketName
     * @param folderName
     * @param client
     */
    public static void createFolder(String bucketName, String folderName, S3Client client) {
    	final String SUFFIX = "/";
    	
    	
    	 // Create an empty folder 
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(folderName + SUFFIX) 
            .contentLength(0L)
            .build();
    	
        // Use an empty body to simulate folder creation
        PutObjectResponse response = client.putObject(putObjectRequest, RequestBody.fromBytes(new byte[0]));
        
        if (response.sdkHttpResponse().isSuccessful()) {
        	log.info("Folder created: " + folderName);
        } else {
        	log.error("Failed to create folder: " + folderName);
        }
    }
    
	private String getAccountIDUsingAccessKey(String accessKey, String secretKey) {
		// Do NOT close this StsClient. DefaultCredentialsProvider.create() is a JVM-wide singleton;
		// closing any client built with it shuts down the shared IRSA STS connection pool, which
		// then breaks credential refresh for every other client (incl. DynamoDB) after ~1h. This
		// runs once at startup, so the single un-closed client lives harmlessly for the app lifetime.
		StsClient stsClient = StsClient.builder().credentialsProvider(DefaultCredentialsProvider.create())
				.build();
		GetCallerIdentityResponse callerIdentity = stsClient
				.getCallerIdentity(GetCallerIdentityRequest.builder().build());
		return callerIdentity.account();
	}
    
}
