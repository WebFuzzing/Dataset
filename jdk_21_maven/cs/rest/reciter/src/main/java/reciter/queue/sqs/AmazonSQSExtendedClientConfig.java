package reciter.queue.sqs;

/*@Slf4j
@Configuration
@ComponentScan(basePackages ="reciter.queue.sqs")
public class AmazonSQSExtendedClientConfig {
	
	private String amazonAWSAccessKey = System.getenv("AMAZON_AWS_ACCESS_KEY");

    private String amazonAWSSecretKey = System.getenv("AMAZON_AWS_SECRET_KEY");
    
    @Value("${aws.sqs.use}")
    private boolean isAwsSqs;
    
    @Value("${aws.sqs.region}")
    private String awsSQSRegion;
    
    @Value("${aws.sqs.extendedClient}")
    private boolean isAwsSqsExtendedClient;
    
    @Value("${aws.sqs.s3.bucketName}")
    private String s3BucketName;
    
    @Value("${aws.s3.region}")
    private String awsS3Region;
    
    @Value("${aws.dynamoDb.local}")
    private boolean isDynamoDbLocal;
    
    @Autowired(required=false)
    private AmazonS3 s3;
    
    @Bean
    @Scope("singleton")
    public AmazonSQS amazonSQSExtendedClient() {
    	
    	if(!isDynamoDbLocal && isAwsSqs && isAwsSqsExtendedClient) {
    		//AmazonS3 s3 = ApplicationContextHolder.getContext().getBean(AmazonS3.class);
    		
    		
             * Set the Amazon S3 bucket name, and then set a lifecycle rule on the
             * bucket to permanently delete objects 14 days after each object's
             * creation date.
             
    		final BucketLifecycleConfiguration.Rule expirationRule =
                    new BucketLifecycleConfiguration.Rule();
            expirationRule.withExpirationInDays(7).withStatus("Enabled");
            
            final BucketLifecycleConfiguration lifecycleConfig =
                    new BucketLifecycleConfiguration().withRules(expirationRule);
    		
    		if(s3.doesBucketExistV2(s3BucketName.toLowerCase())) {
    			log.info(s3BucketName.toLowerCase() + " Bucket Name already exists");
    		} else {
    			try {
    				s3.createBucket(s3BucketName.toLowerCase());
    				s3.setBucketLifecycleConfiguration(s3BucketName.toLowerCase(), lifecycleConfig);
    			} catch(AmazonS3Exception e) {
    				log.error(e.getErrorMessage());
    			}
    		}
    		
    		final ExtendedClientConfiguration extendedClientConfig = new ExtendedClientConfiguration().withLargePayloadSupportEnabled(s3, s3BucketName)
    				.withMessageSizeThreshold(256);
    		
    		final AmazonSQS sqs = new AmazonSQSExtendedClient(AmazonSQSClientBuilder
					  .standard()
					  .withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials() {
						
						@Override
						public String getAWSSecretKey() {
							return amazonAWSSecretKey;
						}
						
						@Override
						public String getAWSAccessKeyId() {
							return amazonAWSAccessKey;
						}
					}))
					.withRegion(awsSQSRegion)
					.build(), extendedClientConfig);
    				
    		return sqs;
    	} else if(isAwsSqs && !isAwsSqsExtendedClient && !isDynamoDbLocal) {
    		//regular client
	    		final AmazonSQS sqs = AmazonSQSClientBuilder
						  .standard()
						  .withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials() {
							
							@Override
							public String getAWSSecretKey() {
								return amazonAWSSecretKey;
							}
							
							@Override
							public String getAWSAccessKeyId() {
								return amazonAWSAccessKey;
							}
						}))
						.withRegion(awsSQSRegion)
						.build();
	  				
	  		return sqs;
    	}
    	
    	return null;
    }

}*/
