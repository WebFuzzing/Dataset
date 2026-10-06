package com.example.demo.auth.config;

import com.example.demo.auth.model.enums.ConfigurationParameter;
import com.example.demo.auth.utils.KeyConverter;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Configuration class for token-related parameters.
 * This class retrieves and initializes token configuration settings, including
 * issuer, access token expiration, refresh token expiration, public key, and private key.
 * The parameters are loaded from predefined configuration constants.
 */
@Getter
@Configuration
public class TokenConfigurationParameter {

    private final String issuer;
    private final int accessTokenExpireMinute;
    private final int refreshTokenExpireDay;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    /**
     * Initializes the token configuration parameters.
     * <p>
     * The parameters are retrieved from the {@link ConfigurationParameter} class:
     * <ul>
     *   <li>Issuer: {@code ConfigurationParameter.ISSUER}</li>
     *   <li>Access token expiration (minutes): {@code ConfigurationParameter.AUTH_ACCESS_TOKEN_EXPIRE_MINUTE}</li>
     *   <li>Refresh token expiration (days): {@code ConfigurationParameter.AUTH_REFRESH_TOKEN_EXPIRE_DAY}</li>
     *   <li>Public key: {@code ConfigurationParameter.AUTH_PUBLIC_KEY}</li>
     *   <li>Private key: {@code ConfigurationParameter.AUTH_PRIVATE_KEY}</li>
     * </ul>
     * Key values are converted using the {@link KeyConverter} utility class.
     * </p>
     */
    public TokenConfigurationParameter() {

        this.issuer = ConfigurationParameter.ISSUER.getDefaultValue();

        this.accessTokenExpireMinute = Integer.parseInt(
                ConfigurationParameter.AUTH_ACCESS_TOKEN_EXPIRE_MINUTE.getDefaultValue()
        );

        this.refreshTokenExpireDay = Integer.parseInt(
                ConfigurationParameter.AUTH_REFRESH_TOKEN_EXPIRE_DAY.getDefaultValue()
        );

        this.publicKey = KeyConverter.convertPublicKey(
                ConfigurationParameter.AUTH_PUBLIC_KEY.getDefaultValue()
        );

        this.privateKey = KeyConverter.convertPrivateKey(
                ConfigurationParameter.AUTH_PRIVATE_KEY.getDefaultValue()
        );

    }

}
