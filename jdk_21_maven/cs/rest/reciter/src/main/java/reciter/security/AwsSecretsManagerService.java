//package reciter.security;
//
//import java.util.Optional;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
//
///**
// * @author mjangari
// * Helps in retrieving the Keys and values from the AWS Secrets Manager.
// */
//@Service
//public class AwsSecretsManagerService {
//
//	private static final Logger log = LoggerFactory.getLogger(AwsSecretsManagerService.class);
//	
//	private static final ObjectMapper objectMapper = new ObjectMapper();  
//	
//	
//	@Value("${aws.secretsmanager.region}")
//    private String secretManagerRegion;
//	
//	public Optional<SecretsManagerClient> createSecretsManagerClient()
//	{
//		return Optional.of(
//				    SecretsManagerClient.builder()
//				        .region(secretManagerRegion!=null ?Region.of(secretManagerRegion):Region.of("us-east-1")) // in any case if SecretsManager is null then read from the us-east-1 region
//				        .credentialsProvider(DefaultCredentialsProvider.create())
//				        .build()
//				);
//	}
//	
//    public JsonNode getSecrets(String secretName) {
//    	
//    	SecretsManagerClient secretsManagerClient = createSecretsManagerClient() 
//    												.orElseThrow(() -> new IllegalStateException("SecretsManagerClient could not be created: region is missing"));
//
//        log.info("secretName pulled from application.properties" + secretName);
//        // Fetch the secret from AWS Secrets Manager
//        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
//                .secretId(secretName)
//                .build();
//
//        GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
//        
//        
//        // Assuming the secret is in JSON format, parse the client secrets from it
//        JsonNode clientSecrets = parseJsonSecrets(valueResponse.secretString());
//     
//        return clientSecrets;  // Return the client secret based on the clientId
//     }
//
//    private JsonNode parseJsonSecrets(String secretJson) {
// 
//         try {
//        	 if(secretJson!=null && !secretJson.equalsIgnoreCase(""))
//        	 {
//        		 // Parse the secret string into a JsonNode (Jackson's tree model)
//                 JsonNode jsonNode = objectMapper.readTree(secretJson);
//                 return jsonNode;
//        	 }
//        	 else {
//                 throw new Exception("Secret not found or is empty");
//             }
//
//         } catch (JsonMappingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        return null; 
//    }
//    
//    public String getSecretKeyPairs(String secretName) {
// 
//    	SecretsManagerClient secretsManagerClient = createSecretsManagerClient() 
//				.orElseThrow(() -> new IllegalStateException("SecretsManagerClient could not be created: region is missing"));
//
//
//        log.info("secretName1 pulled from application.properties" + secretName);
//        // Fetch the secret from AWS Secrets Manager
//        GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
//                .secretId(secretName)
//                .build();
//
//        GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
//        
//        
//        // Assuming the secret is in JSON format, parse the client secrets from it
//        String clientSecrets = valueResponse.secretString();
//     
//        return clientSecrets;  // Return the client secret based on the clientId
//     }
//    
//    public JsonNode getSecretValueFromSecretsManager(String secretName, String secretKey) {
//    	 JsonNode secretValueJson = getSecrets(secretName); 
//         if (secretValueJson != null && secretKey!=null && !secretKey.equalsIgnoreCase("")) {
//        	return secretValueJson.get(secretKey); 
//	    }
//        return null;
//    }
//}
//
