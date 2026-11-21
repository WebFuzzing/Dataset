package it.pagopa.pn.ciechecker.generator.challenge;

import it.pagopa.pn.ciechecker.CieChecker;
import it.pagopa.pn.ciechecker.CieCheckerInterface;
import it.pagopa.pn.ciechecker.model.CieIas;
import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.ciechecker.utils.ValidateUtils;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static it.pagopa.pn.ciechecker.utils.CieCheckerConstants.OK;
import static it.pagopa.pn.ciechecker.utils.ValidateUtils.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@lombok.CustomLog
@SpringBootTest(classes = {it.pagopa.pn.ciechecker.CieCheckerImpl.class, it.pagopa.pn.ciechecker.client.s3.S3BucketClientImpl.class})
@ActiveProfiles("test")
@EnableConfigurationProperties(PnMandateConfig.class)
public class ChallengeResponseBuilderTest {

    @Autowired
    private CieChecker cieChecker;
    @Autowired
    private CieCheckerInterface cieCheckerInterface;


    private static final Path basePath= Path.of("src","test","resources");

    private static final Path privatekeyPathTest =  basePath.resolve("catest.key");
    private static final Path certificatoPathTest = basePath.resolve("catest.pem");

    @Test
    void generateChallengeTest() throws Exception {

        log.info("TEST generateSignedNonce - INIT... ");

        byte[] certificatoByte = Files.readAllBytes(certificatoPathTest);
        byte[] privateKeyByte = Files.readAllBytes(privatekeyPathTest);
        assertNotNull(certificatoByte);
        assertNotNull(privateKeyByte);

        X509Certificate certX509 = (X509Certificate) CertificateFactory.getInstance("X.509", new BouncyCastleProvider())
                .generateCertificate(new ByteArrayInputStream(certificatoByte));
        //X509CertificateHolder holder = new JcaX509CertificateHolder(certX509);

        System.out.println("Certificato caricato: " + certX509.getSerialNumber());

        PrivateKey privateKey = ValidateUtils.parsePrivateKey(privateKeyByte);

        byte[] publicKeyByte = certX509.getPublicKey().getEncoded();
        SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(publicKeyByte);
        byte[] rawRsaKeyBytes = spki.getPublicKeyData().getBytes();

        CieIas ias = new CieIas();
        ias.setPublicKey( rawRsaKeyBytes ); //publicKeyByte);
        CieValidationData validationData = new CieValidationData();
        validationData.setNonce(cleanString(basePath.resolve("NONCE.txt"))); //m
        validationData.setCieIas(ias);

        assertNotNull(validationData.getCieIas().getPublicKey());
        assertNotNull(validationData.getNonce());

        byte[] signedNonce = ChallengeResponseBuilder.signNonce(validationData.getNonce(), privateKey);

        validationData.setSignedNonce(signedNonce);
        String signedNonceBase64 = Base64.getEncoder().encodeToString(validationData.getSignedNonce());

        System.out.println("Nonce: " + validationData.getNonce());
        System.out.println("SignedNonce (Base64): " + signedNonceBase64);

        ResultCieChecker result = cieCheckerInterface.verifyChallengeFromSignature(validationData);
        Assertions.assertEquals(result.getValue(), OK);

        log.info("TEST generateSignedNonce - END ");
    }



}
