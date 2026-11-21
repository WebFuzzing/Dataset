package it.pagopa.pn.ciechecker.generator.sod;

import it.pagopa.pn.ciechecker.CieChecker;
import it.pagopa.pn.ciechecker.CieCheckerInterface;
import it.pagopa.pn.ciechecker.client.s3.S3BucketClient;
import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.model.CieMrtd;
import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.ciechecker.utils.ValidateUtils;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.openssl.PEMParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest(classes = {it.pagopa.pn.ciechecker.CieCheckerImpl.class, it.pagopa.pn.ciechecker.client.s3.S3BucketClientImpl.class})
@Slf4j
@ActiveProfiles("test")
@EnableConfigurationProperties(PnMandateConfig.class)
class SodMrtdBuilderTest {


    @Autowired
    private CieChecker cieChecker;
    @Autowired
    private CieCheckerInterface cieCheckerInterface;
    @MockitoBean
    private S3BucketClient s3BucketClient;

    private static final Path basePath = Path.of("src", "test", "resources");
    private static final String MASTERLIST_NAME="new_IT_MasterListCSCA.zip";
    private static final String CA_FILENAME="catest.pem";
    private static final String KEY_FILENAME="catest.key";

    //PARAMETERS
    private static final String GIVEN_NAME = "MARIO";
    private static final String SURNAME = "ROSSI";
    private static final String DOCUMENT_NUMBER = "AA1234567";
    private static final String NATIONALITY = "ITA";
    private static final String COD_FISCALE="RSSMRA80A01H501U";
    private static final String PLACE_OF_BIRTH= "ROMA";
    private static final char SEX= 'M';
    private static final LocalDate DATE_OF_BIRTH= LocalDate.of(1980,1,1);
    private static final LocalDate EXPIRY_DATE=  LocalDate.of(2030, 12, 31);



    @BeforeEach
    void setUp() throws Exception {
        InputStream inputStream = new FileInputStream(Path.of(basePath.toString(), MASTERLIST_NAME).toFile());
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(inputStream)
        );
        when(s3BucketClient.getObjectContent(anyString())).thenAnswer(i ->s3Stream);

        cieChecker.init();
    }


    @Test
    void sodBuilderTest() throws Exception {
        X509Certificate caCert= loadPemCert(basePath.resolve(CA_FILENAME));
        PrivateKey caPrivateKey = loadPemKey(basePath.resolve(KEY_FILENAME));

        CieMrtd mrtd = getCieMrtd(caCert, caPrivateKey);
        byte []truncSod = Arrays.copyOfRange(mrtd.getSod(), 4, mrtd.getSod().length);
        mrtd.setSod(truncSod);
        CMSSignedData cms = new CMSSignedData( mrtd.getSod());
        verifyMrtdSod(cms,mrtd);
    }

    @Test
    void verifyCodiceFiscaleDeleganteTest() throws Exception {
        X509Certificate caCert= loadPemCert(basePath.resolve(CA_FILENAME));
        PrivateKey caPrivateKey = loadPemKey(basePath.resolve(KEY_FILENAME));

        CieMrtd mrtd = getCieMrtd(caCert, caPrivateKey);
        CieValidationData data = new CieValidationData();

        data.setCieMrtd(mrtd);
        data.setCodFiscDelegante(COD_FISCALE);

        assertEquals(ResultCieChecker.OK, cieCheckerInterface.verifyCodFiscDelegante(data));
        data.setCodFiscDelegante("WRONG");
        Assertions.assertThrows(CieCheckerException.class,()-> cieCheckerInterface.verifyCodFiscDelegante(data));
    }

    @Test
    void verifyExpirationCieTest() throws Exception {
        X509Certificate caCert= loadPemCert(basePath.resolve(CA_FILENAME));
        PrivateKey caPrivateKey = loadPemKey(basePath.resolve(KEY_FILENAME));
        CieMrtd mrtd = getCieMrtd(caCert, caPrivateKey);

        assertEquals(ResultCieChecker.OK,cieCheckerInterface.verifyExpirationCie(mrtd.getDg1()));
    }

    private CieMrtd getCieMrtd(X509Certificate caCert, PrivateKey caPrivateKey) throws Exception {
        SodMrtdBuilder sb = new SodMrtdBuilder();

        return sb.buildCieMrtdAndSignSodWithDocumentSigner(
                SURNAME,
                GIVEN_NAME,
                DOCUMENT_NUMBER,
                NATIONALITY,
                DATE_OF_BIRTH,
                SEX,
                EXPIRY_DATE,
                COD_FISCALE,
                PLACE_OF_BIRTH,
                caPrivateKey,
                caCert
        );
    }

    private static X509Certificate loadPemCert(Path pemPath) throws Exception {
        try (var r = Files.newBufferedReader(pemPath);
             var pp = new PEMParser(r)) {
            Object o = pp.readObject();
            X509CertificateHolder h = (X509CertificateHolder) o;
            return new JcaX509CertificateConverter().getCertificate(h);
        }
    }

    private static PrivateKey loadPemKey(Path keyPath) throws Exception {
        try (var r = Files.newBufferedReader(keyPath);
             var pp = new PEMParser(r)) {
            Object o = pp.readObject();
            JcaPEMKeyConverter conv = new JcaPEMKeyConverter();
            if (o instanceof PrivateKeyInfo pki) {
                return conv.getPrivateKey(pki);
            } else if (o instanceof org.bouncycastle.openssl.PEMKeyPair kp) {
                return conv.getKeyPair(kp).getPrivate();
            } else {
                throw new IllegalArgumentException("Unsupported key format: " + o.getClass());
            }
        }
    }

    private void verifyMrtdSod(CMSSignedData cms,CieMrtd mrtd){
        ResultCieChecker dsr = ValidateUtils.verifyDigitalSignature(cms);
        assertEquals(ResultCieChecker.OK, dsr, "Raw CMS signature must be valid");

        ResultCieChecker integrityResult = cieCheckerInterface.verifyIntegrity(mrtd);
        assertEquals(ResultCieChecker.OK, integrityResult, "MRZ/DG digests must match");

        ResultCieChecker signatureResult = cieCheckerInterface.verifyDigitalSignature(cms);
        assertEquals(ResultCieChecker.OK, signatureResult, "CieChecker signature validation must pass");
    }

}
