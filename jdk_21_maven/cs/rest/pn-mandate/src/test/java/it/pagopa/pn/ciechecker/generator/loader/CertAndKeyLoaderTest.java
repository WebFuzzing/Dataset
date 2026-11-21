package it.pagopa.pn.ciechecker.generator.loader;

import it.pagopa.pn.mandate.config.PnMandateConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest(classes = {it.pagopa.pn.ciechecker.CieCheckerImpl.class, it.pagopa.pn.ciechecker.client.s3.S3BucketClientImpl.class})
@EnableConfigurationProperties(PnMandateConfig.class)
class CertAndKeyLoaderTest {

    @MockitoSpyBean
    private PnMandateConfig config;
    @MockitoBean
    private S3Client s3;

    private static final String CERT_AND_KEY_ZIP_PATH=  "src/test/resources/ca_and_key.zip";


    @BeforeEach
    void setUp() throws IOException {
        System.setProperty("cie.generator.bucket","bucket");
        System.setProperty("cie.generator.file-key","key");

        InputStream fileInputStream = new FileInputStream(Path.of(CERT_AND_KEY_ZIP_PATH).toFile());
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(fileInputStream)
        );

        when(s3.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> s3Stream);


    }
/*
    @Test
    void loadFromS3Test() throws IOException, GeneralSecurityException {
        Assertions.assertNotNull(config.getGeneratorBucketName());
        Assertions.assertNotNull(config.getGeneratorZipName());

        CertAndKeyLoader loader = new CertAndKeyLoader();
        CertAndKey certAndKey = loader.loadCaAndKeyFromS3();
        Assertions.assertNotNull(certAndKey);
        Assertions.assertNotNull(certAndKey.keyPair().getPrivate());
        Assertions.assertNotNull(certAndKey.keyPair().getPublic());
        Assertions.assertNotNull(certAndKey.certificate());
    }

 */


}
