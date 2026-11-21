package com.arcane.dam.security;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import com.arcane.dam.dto.AuthResponseDTO;
import com.arcane.dam.dto.AuthenticatedUserData;
import com.arcane.dam.dto.OTPVerificationRequest;
import com.arcane.dam.dto.UsersDTO;
import com.arcane.dam.entity.Users;
import com.arcane.dam.repository.AuthRepository;
import com.arcane.dam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CognitoService {

    @Value("${spring.security.oauth2.client.registration.cognito.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.cognito.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.cognito.scope}")
    private String scope;

    @Value("${spring.security.oauth2.client.provider.cognito.issuer-uri}")
    private String issuerUri;

    private final AWSCognitoIdentityProvider cognitoIdentityProvider;
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final ModelMapper mapper;

    public UsersDTO registerUser(UsersDTO usersDTO) {
        try {

            String secretHash = generateSecretHash(clientId, clientSecret, usersDTO.getEmail());

            // Create the SignUpRequest
            SignUpRequest signUpRequest = new SignUpRequest()
                    .withClientId(clientId)
                    .withUsername(usersDTO.getEmail())
                    .withPassword(usersDTO.getPassword())
                    .withUserAttributes(
                            new AttributeType().withName("email").withValue(usersDTO.getEmail())
                    )
                    .withSecretHash(secretHash);

            cognitoIdentityProvider.signUp(signUpRequest);

            Users registeredUser = mapper.map(usersDTO, Users.class);
            return mapper.map(userRepository.save(registeredUser), UsersDTO.class);

        } catch (Exception e) {
            throw new RuntimeException("User registration failed: " + e.getMessage());
        }
    }

    public AuthResponseDTO verifyUser(OTPVerificationRequest request) {
        try {
            String secretHash = generateSecretHash(clientId, clientSecret, request.getUserName());

            ConfirmSignUpRequest confirmSignUpRequest = new ConfirmSignUpRequest()
                    .withClientId(clientId)
                    .withUsername(request.getUserName())
                    .withConfirmationCode(request.getOtp())
                    .withSecretHash(secretHash);

            ConfirmSignUpResult confirmSignUpResult = cognitoIdentityProvider.confirmSignUp(confirmSignUpRequest);

            int httpStatusCode = confirmSignUpResult.getSdkHttpMetadata().getHttpStatusCode();
            if (httpStatusCode == 200) {

                // proceed with user verification
                Users user = userRepository.findUserByEmail(request.getUserName());
                user.setIsEnabled(true);
                userRepository.update(user.getId(),user);



                return new AuthResponseDTO();
            } else {
                throw new RuntimeException("Unexpected HTTP status code: " + httpStatusCode);
            }
        } catch (CodeMismatchException e) {
            throw new RuntimeException("Invalid OTP: " + e.getMessage(), e);
        } catch (ExpiredCodeException e) {
            throw new RuntimeException("OTP has expired: " + e.getMessage(), e);
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("User verification failed: " + e.getMessage(), e);
        }
    }


    public boolean resendOtp(String username) {
        try {
            String secretHash = generateSecretHash(clientId, clientSecret, username);
            ResendConfirmationCodeRequest resendRequest = new ResendConfirmationCodeRequest()
                    .withClientId(clientId)
                    .withUsername(username)
                    .withSecretHash(secretHash);

            cognitoIdentityProvider.resendConfirmationCode(resendRequest);
            System.out.println("OTP resent successfully to user: " + username);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to resend OTP: " + e.getMessage(), e);
        }
    }


    public AuthResponseDTO loginUser(String email, String password) {
        try {

            String secretHash = generateSecretHash(clientId, clientSecret, email);

            InitiateAuthRequest authRequest = new InitiateAuthRequest()
                    .withAuthFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .withClientId(clientId)
                    .withAuthParameters(Map.of(
                            "USERNAME", email,
                            "PASSWORD", password,
                            "SECRET_HASH", secretHash
                    ));

            InitiateAuthResult authResult = cognitoIdentityProvider.initiateAuth(authRequest);

            AuthenticationResultType authResponse = authResult.getAuthenticationResult();
            if (authResponse == null) {
                throw new RuntimeException("Authentication failed: No authentication result returned.");
            }

            Users  user = authRepository.findUserByEmail(email);
            return new AuthResponseDTO(authResponse.getAccessToken(), new AuthenticatedUserData(
                    user.getId(),
                    user.getUserName(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getSpaces(),
                    user.getCreatedAt(),
                    user.getIsEnabled()
            ));

        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Invalid credentials: " + e.getMessage(), e);
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User does not exist in Cognito: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error during login: " + e.getMessage(), e);
        }
    }

    private String generateSecretHash(String clientId, String clientSecret, String username) {
        try {
            String message = username + clientId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(clientSecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating SECRET_HASH: " + e.getMessage(), e);
        }
    }
}
