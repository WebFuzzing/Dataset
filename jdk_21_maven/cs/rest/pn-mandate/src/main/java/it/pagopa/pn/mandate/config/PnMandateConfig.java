package it.pagopa.pn.mandate.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI; // MODIFIED
import java.time.Duration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pn.mandate")
@Import(SharedAutoConfiguration.class)
@EnableScheduling
public class PnMandateConfig {

    private String clientExtregBasepath;
    private String clientDatavaultBasepath;
    private String clientDeliveryBasepath;

    @Value("${aws.dynamodb_table:}")
    private String dynamodbTable;
    @Value("${aws.dynamodb_table_history:}")
    private String dynamodbTableHistory;

    private Integer maxPageSize;

    private Duration pendingDuration;
    private Duration ciePendingDuration;
    private Duration cieValidToDuration;

    private Boolean revokeCieMandateOnVerificationFailure;

    private String ciecheckerCscaAnchorPathFilename;

    @Value("${aws.region-code}")
    private String regionCode;

    // MODIFIED: allow overriding S3 endpoint for LocalStack
    @Value("${aws.endpoint-url:}")
    private String endpointUrl;

    private String generatorBucketName;
    private String generatorZipName;

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER_V2 = DefaultCredentialsProvider.create();

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder clientBuilder = configureBuilder(S3Client.builder());
        // MODIFIED: use path-style access when targeting custom S3 endpoint (LocalStack)
        if (this.endpointUrl != null && !this.endpointUrl.isBlank()) {
            clientBuilder.forcePathStyle(true);
        }
        return clientBuilder.build();
    }

    public <B extends AwsClientBuilder<?, ?>> B configureBuilder(B builder) {
        log.info("Using DefaultCredentialsProvider for AWS credentials");
        builder.credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER_V2);

        // verifichiamo se aws.region-code è valorizzata nel caso la impostiamo
        if(this.regionCode != null) {
            log.info("AWS regionCode configurata: {}", regionCode);
            builder.region(Region.of(this.regionCode));
        } else {
            log.warn("AWS regionCode non è valorizzata, viene usato il default");
        }
        // MODIFIED: route S3 client to custom endpoint when configured
        if (this.endpointUrl != null && !this.endpointUrl.isBlank()) {
            builder.endpointOverride(URI.create(this.endpointUrl));
        }
        return builder;
    }



}
