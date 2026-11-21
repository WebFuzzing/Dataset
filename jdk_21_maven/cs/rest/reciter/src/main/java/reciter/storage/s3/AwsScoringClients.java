package reciter.storage.s3;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Shared, application-lifetime AWS clients used by the scoring path.
 *
 * <p>{@link DefaultCredentialsProvider#create()} returns a JVM-wide singleton that is
 * {@code SdkAutoCloseable}. Every client built with it shares that one provider and its single
 * IRSA STS ({@code AssumeRoleWithWebIdentity}) connection pool. Closing any such client shuts
 * that pool down, so once the ~1h IRSA session expires every other client — including the
 * long-lived DynamoDB client — fails credential refresh with "Connection pool shut down".
 *
 * <p>These clients are therefore built once and never closed. SDK v2 clients are thread-safe and
 * meant to be shared for the app lifetime (AWS best practice); the JVM reclaims them at shutdown.
 */
public final class AwsScoringClients {

    private AwsScoringClients() {}

    private static volatile S3Client s3;
    private static volatile LambdaClient lambda;

    public static S3Client s3() {
        S3Client c = s3;
        if (c == null) {
            synchronized (AwsScoringClients.class) {
                c = s3;
                if (c == null) {
                    c = S3Client.builder()
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .region(Region.of(System.getenv("AWS_REGION")))
                            .build();
                    s3 = c;
                }
            }
        }
        return c;
    }

    public static LambdaClient lambda(String region) {
        LambdaClient c = lambda;
        if (c == null) {
            synchronized (AwsScoringClients.class) {
                c = lambda;
                if (c == null) {
                    c = LambdaClient.builder()
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .region(Region.of(region))
                            .build();
                    lambda = c;
                }
            }
        }
        return c;
    }
}
