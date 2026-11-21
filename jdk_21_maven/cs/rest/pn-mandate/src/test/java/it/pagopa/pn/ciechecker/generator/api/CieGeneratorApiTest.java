package it.pagopa.pn.ciechecker.generator.api;


import it.pagopa.pn.ciechecker.CieChecker;
import it.pagopa.pn.ciechecker.CieCheckerImpl;
import it.pagopa.pn.ciechecker.client.s3.S3BucketClient;
import it.pagopa.pn.ciechecker.generator.loader.CertAndKeyLoader;
import it.pagopa.pn.ciechecker.generator.model.CertAndKey;
import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.zip.ZipInputStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {CieGeneratorApi.class, S3Client.class, CieCheckerImpl.class})
@Slf4j
@ActiveProfiles("test")
@EnableConfigurationProperties(PnMandateConfig.class)
public class CieGeneratorApiTest {

    @MockitoSpyBean
    private PnMandateConfig config;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3BucketClient s3BucketClient;

    @Autowired
    private CieChecker cieChecker;

    private static final Path basePath = Path.of("src", "test", "resources");
    private static final Path outputDir = Path.of("src", "test", "resources", "output-dir");

    private static final String CA_AND_KEY_ZIP = "ca_and_key.zip";
    //PARAMETERS
    private static final String COD_FISCALE = "RSSMRA80A01H501U";
    private static final String NONCE = "02461";
    private static final LocalDate EXPIRY_DATE = LocalDate.of(2030, 12, 31);
    public static final String NEW_CSCA_ANCHOR_PATH_FILENAME = "src/test/resources/new_IT_MasterListCSCA.zip";
    public static final String CA_AND_KEY_PATH_FILENAME = "src/test/resources/ca_and_key.zip";


    CieGeneratorApiImpl cieGeneratorApi;
    CertAndKeyLoaderTest certAndKeyLoader;


    @BeforeEach
    void setUp() throws Exception {
        cieGeneratorApi = new CieGeneratorApiImpl();
        certAndKeyLoader = new CertAndKeyLoaderTest(Path.of(CA_AND_KEY_PATH_FILENAME));

        ReflectionTestUtils.setField(cieGeneratorApi, "certAndKeyLoader", certAndKeyLoader);


        InputStream fileInputStream = new FileInputStream(Path.of(NEW_CSCA_ANCHOR_PATH_FILENAME).toFile());
        ResponseInputStream<GetObjectResponse> s3BucketClientStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(fileInputStream)
        );
        when(s3BucketClient.getObjectContent(anyString()))
                .thenAnswer(invocation -> s3BucketClientStream);

        cieChecker.init();
    }


    @Test
    void cieValidationDataMandateTest() {
        CieValidationData data = cieGeneratorApi.generateCieValidationData(outputDir.toAbsolutePath(), COD_FISCALE, COD_FISCALE,EXPIRY_DATE, NONCE);
        Assertions.assertEquals(ResultCieChecker.OK,cieChecker.validateMandate(data));
    }

    private class CertAndKeyLoaderTest extends CertAndKeyLoader {

        Path cscaFilePath;

        public CertAndKeyLoaderTest() {

        }

        public CertAndKeyLoaderTest(Path cscaFilePath) {
            this.cscaFilePath = cscaFilePath;
        }

        public CertAndKey loadIssuerCertAndKeyFromS3() throws IOException, GeneralSecurityException {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(cscaFilePath.toFile()));
            return extractCertificateFromZip(zipInputStream);
        }
    }
}
