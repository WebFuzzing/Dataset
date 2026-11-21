package reciter.consumer.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reciter.consumer.dto.TokenInfo;
import reciter.consumer.response.ApiResponse;
import reciter.consumer.service.CognitoAuthService;

/**
 * REST controller that exposes endpoints for obtaining authentication tokens from AWS Cognito.
 *
 * <p>This controller receives client requests containing an API key in the headers, delegates
 * token retrieval to the {@link CognitoTokenService}, and returns the appropriate token or
 * error response. It is responsible for validating input headers, handling exceptions gracefully,
 * and exposing a clean API interface for authentication clients.</p>
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Accepts API key via request headers</li>
 *   <li>Delegates token resolution and caching to the service layer</li>
 *   <li>Returns structured success or error responses to the client</li>
 *  </ul>
 *
 * <p>Example Request:</p>
 * <pre>{@code
 * GET /auth/token
 * Header: APIKey: your-api-key
 * }</pre>
 *
 * <p>Example Response:</p>
 * <pre>{@code
 * 200 OK
 * {
 *   "accessToken": "eyJraWQiOiJrZC1x...",
 *   "expiryTime": "2025-09-15T20:30:00Z"
 *   etc
 * }
 * }</pre>
 *
 * <p>If the API key is missing or invalid, an appropriate error response is returned (e.g., 401).</p>
 *
 * @author mjangari
 * @version 1.0
 * @since 2025-09-15
 */
@RestController
@RequestMapping("/reciter")
public class CognitoAccessTokenController {

	private static final Logger log = LoggerFactory.getLogger(CognitoAccessTokenController.class);
	
	@Autowired
    private CognitoAuthService cognitoAuthService; 
	
	
    @PostMapping(value = "/generate-access-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TokenInfo>> generateAccessToken(@RequestBody Map<String, String> payload) {
       
    	// 1. Extract values from the Map
        String user = payload.get("userName");
        String pass = payload.get("password");

        
        // 2. Validate the fields are present
        if (user == null || pass == null || user.isBlank() || pass.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Payload must contain 'userName' and 'password'", null, 400));
        }
    	
    	try
    	{
    		String tokenInfo = cognitoAuthService.authenticateConsumer(user,pass);
    		return ResponseEntity.status(200).body(new ApiResponse<>(tokenInfo, null, 200));
    	}
    	catch(RuntimeException ak)
    	{
    		return ResponseEntity.status(401).body(new ApiResponse<>(null, ak.getMessage(), 401));
    	}
 		
    }
}
