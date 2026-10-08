package it.pagopa.pn.papertracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Classe che permette di creare un container Docker di LocalStack.
 * Il container (e quindi la classe) può essere condivisa tra più classi di test.
 * Per utilizzare questa classe, le classi di test dovranno essere annotate con
 *
 * @Import(LocalStackTestConfig.class)
 */
@TestConfiguration
public class LocalStackTestConfig {

    public static LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:1.0.4"))
                    .withServices(DYNAMODB, SQS) // ← AGGIUNTO SQS!
                    .withClasspathResourceMapping("testcontainers/init.sh",
                            "/docker-entrypoint-initaws.d/make-storages.sh", BindMode.READ_ONLY)
                    .withClasspathResourceMapping("testcontainers/credentials",
                            "/root/.aws/credentials", BindMode.READ_ONLY)
                    .withNetworkAliases("localstack")
                    .withNetwork(Network.builder().build())
                    .waitingFor(Wait.forLogMessage(".*Initialization terminated.*", 1)
                            .withStartupTimeout(Duration.ofSeconds(180)));

    static {
        localStack.start();
        System.setProperty("aws.endpoint-url", localStack.getEndpointOverride(DYNAMODB).toString());
        System.setProperty("aws.endpoint", localStack.getEndpointOverride(DYNAMODB).toString());
        System.setProperty("aws.region", localStack.getRegion());
        System.setProperty("aws.accessKey", localStack.getAccessKey());
        System.setProperty("aws.secretKey",  localStack.getSecretKey());
        System.setProperty("spring.cloud.aws.sqs.endpoint", localStack.getEndpointOverride(SQS).toString());
        System.setProperty("spring.cloud.aws.region.static", localStack.getRegion());
        System.setProperty("spring.cloud.aws.credentials.access-key", localStack.getAccessKey());
        System.setProperty("spring.cloud.aws.credentials.secret-key", localStack.getSecretKey());
        System.setProperty("pn.paper-tracker.saveAndNotSendToDeliveryPush", "CON018");

        try {
            System.setProperty("aws.sharedCredentialsFile", new ClassPathResource("testcontainers/credentials").getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
