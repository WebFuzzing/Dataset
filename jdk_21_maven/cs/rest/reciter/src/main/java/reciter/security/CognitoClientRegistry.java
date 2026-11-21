
package reciter.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsResponse;

@Component
public class CognitoClientRegistry {

	private static final Logger log = LoggerFactory.getLogger(CognitoClientRegistry.class);	
	
    // v2: CognitoIdentityProviderClient replaces AWSCognitoIdentityProvider
    private CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    private LoadingCache<String, Set<String>> clientCache;

    @PostConstruct
    public void init() {
         // Initialize AWS Client
    	 // v2: CognitoIdentityProviderClient.builder().region() replaces
         //     AWSCognitoIdentityProviderClientBuilder.standard().withRegion()
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion))
                .build();

        // Initialize Caffeine Cache
        this.clientCache = Caffeine.newBuilder()
                .refreshAfterWrite(15, TimeUnit.MINUTES) // Background refresh every 15m
                .expireAfterWrite(24, TimeUnit.HOURS)    // Cleanup if AWS stays down
                .build(key -> fetchAllClientsFromAws());
    }

    public boolean isAllowed(String clientId) {
        try {
            // Get the set from cache (Key name doesn't matter, we use a constant)
            Set<String> authorizedIds = clientCache.get("AUTHORIZED_CLIENTS");
            return authorizedIds != null && authorizedIds.contains(clientId);
        } catch (Exception e) {
            log.error("Security Bypass Prevention: Error checking Client ID {}", clientId, e);
            return false;
        }
    }

    private Set<String> fetchAllClientsFromAws() {
        Set<String> freshIds = new HashSet<>();
        String nextToken = null;

        try {
            do {
               
                
             // v2: ListUserPoolClientsRequest.builder() replaces new ListUserPoolClientsRequest().withXxx()
                ListUserPoolClientsRequest.Builder reqBuilder = ListUserPoolClientsRequest.builder()
                        .userPoolId(userPoolId)
                        .maxResults(60);
                
                if (nextToken != null) {
                    reqBuilder.nextToken(nextToken);
                }

                
                // v2: ListUserPoolClientsResponse replaces ListUserPoolClientsResult
                ListUserPoolClientsResponse result = cognitoClient.listUserPoolClients(reqBuilder.build());
                
                // Collect the Client IDs
                // v2: result.userPoolClients() replaces result.getUserPoolClients()
                // v2: client.clientId() replaces client.getClientId()
                result.userPoolClients().forEach(client -> freshIds.add(client.clientId()));
                
                // v2: result.nextToken() replaces result.getNextToken()
                nextToken = result.nextToken();
            } while (nextToken != null);

            return Collections.unmodifiableSet(freshIds);
        } catch (Exception e) {
            log.error("Failed to sync with Cognito. Check IAM permissions for 'ListUserPoolClients'.", e);
            throw e; 
        }
    }
}