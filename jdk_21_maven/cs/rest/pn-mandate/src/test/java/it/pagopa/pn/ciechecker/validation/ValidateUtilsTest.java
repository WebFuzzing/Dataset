package it.pagopa.pn.ciechecker.validation;

import it.pagopa.pn.ciechecker.client.s3.S3BucketClientImpl;
import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.model.*;
import it.pagopa.pn.ciechecker.utils.ValidateUtils;
import it.pagopa.pn.mandate.MockAWSObjectsTestConfig;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.ciechecker.utils.CieCheckerConstants.X_509;
import static it.pagopa.pn.ciechecker.utils.ValidateUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = it.pagopa.pn.ciechecker.CieCheckerImpl.class)
@TestPropertySource("classpath:application.properties")
@Slf4j
@ActiveProfiles("test")
@EnableConfigurationProperties(PnMandateConfig.class)
@Import({MockAWSObjectsTestConfig.class})
class ValidateUtilsTest {

    private static final Path basePath = Path.of("src","test","resources");
    private static final Path CSCA_DIR = Path.of("src","test","resources","csca");
    private static final String SOD_IAS_HEX = "SOD_IAS.HEX";  //m
    private static final String SOD_MRTD_PADDING_HEX = "SOD_MRTD_PADDING.HEX";  //m
    private static final String SOD_MRTD_HEX = "SOD_MRTD.HEX";  //m
    private static final String SOD_IAS_PADDING = "SOD_IAS_PADDING.HEX";
    private static final String NIS_PADDING_FILE_NAME ="NIS_PADDING.HEX";
    private static final String EF_SOD_HEX = "EF_SOD.HEX";
    private static final String NIS_PUBKEY_FILENAME = "NIS_PUBKEY.HEX";
    private static final String NIS_HEX_TO_CHECK="393130373138343634363534";
    private static final Path sodFile = Paths.get("src/test/resources/EF.SOD");
    private static final Path dg1Files = Paths.get("src/test/resources/EF.DG1");
    private static final Path dg11Files = Paths.get("src/test/resources/EF.DG11");
    private static final Path dg1FilesCorrupted = Paths.get("src/test/resources/DG1_CORROTTO.HEX");
    private static final Path dg11FilesCorroupted = Paths.get("src/test/resources/DG11_CORROTTO.HEX");
    private static final List<String> compatibleAlgorithms = List.of("SHA_256,SHA_384,SHA_512");
    private static final Path masterListCSCA = Paths.get("src/test/resources/IT_MasterListCSCA.zip");
    private static final Path CATEST_FILE_PATH = Paths.get("src/test/resources/catest.pem");
    private static final String masterListCSCAZip_S3 = "s3://dgs-temp-089813480515/IT_MasterListCSCA.zip";
    private static final Path NONCE_FILE_PATH = Paths.get("src/test/resources/NONCE.txt");
    private static final Path NONCE_SIGNATURE_FILE_PATH=(Paths.get("src/test/resources/NONCE_SIGNATURE.HEX"));
    private static final Path NIS_FILE_NAME = Paths.get("NIS.HEX"); //m

    private static final Map<String,String> expectedIssuer = Map.of(
            "2.5.4.3", "Italian Country Signer CA",
            "2.5.4.11", "National Electronic Center of State Police",
            "2.5.4.10", "Ministry of Interior",
            "2.5.4.6", "IT"
    );

    @MockitoBean
    private S3BucketClientImpl s3BucketClient;
    @MockitoBean
    private S3Client s3Client;

    static CieValidationData validationData;

    @BeforeEach
    void setUp() throws IOException, DecoderException {

        // inizio a creare l'inputStream che deve tornare la chiamata s3
        byte[] fileBytes = Files.readAllBytes(masterListCSCA);

        // crea un ResponseInputStream "reale"
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(new ByteArrayInputStream(fileBytes))
        );

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> s3Stream);


        byte[] nisPubKey = hexFile(cleanString(basePath.resolve(NIS_PUBKEY_FILENAME)));
        byte[] nisSignature = hexFile(cleanString(NONCE_SIGNATURE_FILE_PATH));
        //byte[] nisHexToCheck = hexFile(cleanString(basePath.resolve(NIS_FILE_NAME)));
        byte[] nisHexToCheck = hexFile(cleanString(basePath.resolve("NIS.HEX")));  //m
        byte[] sodIasByteArray = loadSodBytes(basePath.resolve(SOD_IAS_HEX));


        byte[] sodMrtd = Files.readAllBytes(sodFile);
        byte[] dg1 = Files.readAllBytes(dg1Files);
        byte[] dg11 = Files.readAllBytes(dg11Files);

        validationData = new CieValidationData();
        CieIas cieIas = new CieIas();
        cieIas.setPublicKey(nisPubKey);
        cieIas.setNis(nisHexToCheck);
        cieIas.setSod(sodIasByteArray);

        validationData.setCieIas(cieIas);
        validationData.setSignedNonce(nisSignature);
        validationData.setNonce(Files.readString(NONCE_FILE_PATH));

        CieMrtd cMrtd = new CieMrtd();
        cMrtd.setSod(sodMrtd);
        cMrtd.setDg1(dg1);
        cMrtd.setDg11(dg11);
        validationData.setCieMrtd(cMrtd);

    }
/*
    @Test
    void extractDscCertDerTest() throws CMSException {

        CMSSignedData cms = new CMSSignedData(validationData.getCieIas().getSod());
        X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);

        Assertions.assertNotNull(holder);

        Arrays.stream(holder.getIssuer().getRDNs()).forEach(elem ->{
            log.info(elem.getFirst().getType().toString());
            log.info(elem.getFirst().getValue().toString());
        });

        ASN1ObjectIdentifier objectIdentifier = new ASN1ObjectIdentifier("2.5.4.3");

        Arrays.stream(holder.getIssuer().getRDNs(org.bouncycastle.asn1.ASN1ObjectIdentifier.getInstance(objectIdentifier))).allMatch(elem ->{
            String key = elem.getFirst().getType().toString();
            Assertions.assertEquals(expectedIssuer.get(key), elem.getFirst().getValue().toString());
            return true;
        });
    }

 */

    // verifyDscAgainstTrustBundle

// TODO to uncomment when useful test data are available
//    @Test
//    void verifyDscAgainstTrustBundleTest() throws Exception {
//
//        CMSSignedData cms = new CMSSignedData(validationData.getCieIas().getSod());
//        X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);
//        List<byte[]> ders = pickManyDerFromResources(-1);
//        assertNotNull(ders);
//System.out.println("DERS SIZE: " + ders.size());
//        String concatenatedPem = ders.stream()
//                .map(d->new String(toPem(d), StandardCharsets.UTF_8))
//                .collect(Collectors.joining());
//
//
//        byte[] blob = concatenatedPem.getBytes(StandardCharsets.UTF_8);
//
//        CertificateFactory certificateFactory = CertificateFactory.getInstance(X_509);
//
//        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(Files.newInputStream(CATEST_FILE_PATH));
//        assertNotNull(certificate);
//
//        Collection<X509Certificate> anchors = parseCscaAnchors(List.of(blob),certificateFactory);
//        anchors.add(certificate);
//        assertNotNull(anchors);
//        System.out.println("ANCHORS SIZE: " + anchors.size());
//
//        ResultCieChecker resultCieChecker = ValidateUtils.verifyDscAgainstTrustBundle(holder.getEncoded() ,anchors,null);
//
//
//        Assertions.assertTrue(resultCieChecker.getValue().equals(OK));
//    }



    @Test
    void verifyDscAgainstTrustBundleIsFalseTest() throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance(X_509);
        CMSSignedData cms = new CMSSignedData(validationData.getCieIas().getSod());
        X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);
        byte[] dscDer = holder.getEncoded();
        List<byte[]> ders = pickManyDerFromResources(-1);

        String concatenatedPem = ders.stream()
                .map(d->new String(toPem(d), StandardCharsets.UTF_8))
                .collect(Collectors.joining());

        byte[] blob = concatenatedPem.getBytes(StandardCharsets.UTF_8);

        Collection<X509Certificate> anchors = parseCscaAnchors(List.of(blob),certificateFactory);

        X509Certificate dscX509 = (X509Certificate) CertificateFactory.getInstance(X_509)
                .generateCertificate(new ByteArrayInputStream(dscDer));

        List<X509Certificate> wrongBundle = anchors.stream()
                .filter(ca -> {
                    try {
                        dscX509.verify(ca.getPublicKey());
                        return false;
                    } catch (Exception e) {
                        return true;
                    }
                })
                .collect(Collectors.toList());

        //ResultCieChecker result = ValidateUtils.verifyDscAgainstTrustBundle(dscDer, wrongBundle, null);
        //Assertions.assertFalse((result.getValue().equals("OK")));
        Assertions.assertThrows(CieCheckerException.class,
                () -> ValidateUtils.verifyDscAgainstTrustBundle(dscDer, wrongBundle, null));
    }

    /* Test sostituito dal test CieCheckerTest.verifyDscAgainstAnchorBytes_derDsc_pemZIP_true
    @Test
    void verifyDscAgainstAnchorBytes_derDsc_pemBundle_true() throws Exception {

        byte[] pkcs7 = extractCertificateByteArray();
        CMSSignedData cms = new CMSSignedData(pkcs7);
        X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);
        byte[] dscDer = holder.getEncoded();

        List<byte[]> ders = pickManyDerFromResources(-1);
        String concatenatedPem = ders.stream()
                .map(d -> new String(toPem(d), StandardCharsets.UTF_8))
                .collect(Collectors.joining());
        byte[] caBlob = concatenatedPem.getBytes(StandardCharsets.UTF_8);

        ResultCieChecker resultCieChecker =
                ValidateUtils.verifyDscAgainstAnchorBytesOld(dscDer, List.of(caBlob), null);
        log.info("TEST resultCieChecker: {}" , resultCieChecker.getValue());
        Assertions.assertTrue(resultCieChecker.getValue().equals(OK));
    }


    @Test
    void verifyDscAgainstAnchorBytes_pemDsc_pemBundle_true() throws Exception {
        byte[] pkcs7 = extractCertificateByteArray();
        CMSSignedData cms = new CMSSignedData(pkcs7);
        X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);
        byte[] dscPem = toPem(holder.getEncoded());

        List<byte[]> ders = pickManyDerFromResources(-1);
        String concatenatedPem = ders.stream()
                .map(d -> new String(toPem(d), StandardCharsets.UTF_8))
                .collect(Collectors.joining());
        byte[] caBlob = concatenatedPem.getBytes(StandardCharsets.UTF_8);

        ResultCieChecker resultCieChecker =ValidateUtils.verifyDscAgainstAnchorBytesOld(dscPem, List.of(caBlob), null);
        log.info("TEST resultCieChecker: {}", resultCieChecker.getValue());

        Assertions.assertTrue(resultCieChecker.getValue().equals(OK));
    }

    @Test
    void verifyDscAgainstAnchorBytes_false_when_all_parents_removed() throws Exception {
        var cf = CertificateFactory.getInstance(X_509);

        // DSC (DER)
        byte[] pkcs7 = extractCertificateByteArray();
        CMSSignedData cms = new CMSSignedData(pkcs7);
        X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);
        byte[] dscDer = holder.getEncoded();
        X509Certificate dscX509 = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(dscDer));

        // Anchors
        List<byte[]> anchorsDer = pickManyDerFromResources(-1);

        // filtering all valid anchors
        List<byte[]> wrongAnchorBlobs = new ArrayList<>();
        int removed = 0;
        for (byte[] der : anchorsDer) {
            X509Certificate ca = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
            boolean verifies;
            try {
                dscX509.verify(ca.getPublicKey());
                verifies = true;
            } catch (Exception e) {
                verifies = false;
            }
            if (!verifies) wrongAnchorBlobs.add(der); else removed++;
        }
        Assertions.assertTrue(removed > 0);

        //ResultCieChecker resultCieChecker = ValidateUtils.verifyDscAgainstAnchorBytes(dscDer, wrongAnchorBlobs, null);
        //Assertions.assertFalse(resultCieChecker.getValue().equals(OK));
        Assertions.assertThrows(CieCheckerException.class,
                () -> ValidateUtils.verifyDscAgainstAnchorBytesOld(dscDer, wrongAnchorBlobs, null));
    }

    @Test
    void verifyDscAgainstAnchorBytes_edgeCases() throws Exception {
        List<byte[]> ders = pickManyDerFromResources(-1);
        String concatenatedPem = ders.stream()
                .map(d -> new String(toPem(d), StandardCharsets.UTF_8))
                .collect(Collectors.joining());
        byte[] caBlob = concatenatedPem.getBytes(StandardCharsets.UTF_8);

        //Sod null
        //ResultCieChecker resultSODNull =ValidateUtils.verifyDscAgainstAnchorBytes(null, List.of(caBlob), null);
        //Assertions.assertFalse(resultSODNull.getValue().equals(OK));
        Assertions.assertThrows(CieCheckerException.class,
                () ->ValidateUtils.verifyDscAgainstAnchorBytesOld(null, List.of(caBlob), null));


                // anchors null
        byte[] pkcs7 = extractCertificateByteArray();
        CMSSignedData cms = new CMSSignedData(pkcs7);
        byte[] dscDer = ValidateUtils.extractDscCertDer(cms).getEncoded();
        //ResultCieChecker resultNull = ValidateUtils.verifyDscAgainstAnchorBytes(dscDer, null, null);
        //Assertions.assertFalse(resultNull.getValue().equals(OK));
        Assertions.assertThrows(CieCheckerException.class,
                        () ->ValidateUtils.verifyDscAgainstAnchorBytes(dscDer, null, null));


        // anchors empty
        //ResultCieChecker resultEmpty = ValidateUtils.verifyDscAgainstAnchorBytes(dscDer, List.of(), null);
        //Assertions.assertFalse(resultEmpty.getValue().equals(OK));
        Assertions.assertThrows(CieCheckerException.class,
                () -> ValidateUtils.verifyDscAgainstAnchorBytes(dscDer, List.of(), null));

        // anchor malformed
        byte[] malformed = "INVALID".getBytes(StandardCharsets.UTF_8);
        //ResultCieChecker resultMalformed = ValidateUtils.verifyDscAgainstAnchorBytes(dscDer, List.of(malformed), null);
        //Assertions.assertFalse(resultMalformed.getValue().equals(OK));
        Assertions.assertThrows(CieCheckerException.class,
                () -> ValidateUtils.verifyDscAgainstAnchorBytesOld(dscDer, List.of(malformed), null));
    }
*/
    @Test
    void verify_invalid_inputs() {
        log.info(" dsc non-parsable ");
        Assertions.assertThrows(CieCheckerException.class,
                () -> ValidateUtils.verifyDscAgainstTrustBundle(new byte[]{0x01,0x02}, List.of(), null));

        log.info(" dsc len 0 ⇒ false ");
        //Assertions.assertFalse(ValidateUtils.verifyDscAgainstTrustBundle(new byte[0], List.of(), null));
        Assertions.assertThrows(CieCheckerException.class,
                () -> ValidateUtils.verifyDscAgainstTrustBundle(new byte[0], List.of(), null));

        log.info(" anchors null ⇒ false ");
        //Assertions.assertFalse(ValidateUtils.verifyDscAgainstTrustBundle(new byte[]{1}, null, null));
        Assertions.assertThrows(CieCheckerException.class,
                () ->ValidateUtils.verifyDscAgainstTrustBundle(new byte[]{1}, null, null));
    }

    // parseCscaAnchors

    @Test
    void parseCscaAnchorsTest() throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance(X_509);
        List<byte[]> ders = pickManyDerFromResources(-1);

        String concatenatedPem = ders.stream()
                .map(d->new String(toPem(d), StandardCharsets.UTF_8))
                .collect(Collectors.joining());

        byte[] blob = concatenatedPem.getBytes(StandardCharsets.UTF_8);

        List<X509Certificate> anchors = parseCscaAnchors(List.of(blob),certificateFactory);

        Assertions.assertEquals(ders.size(), anchors.size());
    }


//    @Test
//    void extractPublicKeyFromHolder() throws IOException, DecoderException, CMSException {
//        log.info("TEST extractPublicKeyFromHolder - INIT");
//        log.info(" - Leggo il file SOD_HEX e decodifico in byte[] HEX");
////        String fileString = Files.readString(basePath.resolve(SOD_IAS_HEX)).replaceAll("\\s+", "");
////        String subString = fileString.substring(8);
////        byte[] sodIasByteArray = hexFile(subString);
//        byte[] sodIasByteArray = hexFile(Files.readString(basePath.resolve(SOD_IAS_HEX)));
//        byte[] truncatedSODIAS = Arrays.copyOfRange(sodIasByteArray, 4, sodIasByteArray.length);
//        //log.info(" - sodIasByteArray: {}", Arrays.toString(sodIasByteArray));
//
//        CMSSignedData cms = new CMSSignedData(truncatedSODIAS);
//        X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);
//
//        log.info(" - Estrae il certificato X509CertificateHolder: {}", holder);
//        log.info(" - Estrae il certificato X509CertificateHolder: {}" , holder.getSignatureAlgorithm().getAlgorithm());
//        //Estrae il certificato X509CertificateHolder: 1.2.840.113549.1.1.5
//        Assertions.assertNotNull(holder);
//
//        log.info(" - Estrae la publicKey dal certificato X509CertificateHolder");
//        PublicKey publicKey = ValidateUtils.extractPublicKeyFromHolder( holder); //SerialNumber: 1382935226051631925
//        log.info(" - publicKey: {}" , publicKey);
//        log.info(" - publicKey Format: {}" , publicKey.getFormat()); //publicKey Format: X.509
//        log.info(" - publicKey Algorithm: {}" , publicKey.getAlgorithm()); //RSA
//        byte[] certDer = publicKey.getEncoded();  //bytes del certificato DER usato per l’estrazione OpenSSL
//        Assertions.assertNotNull(certDer);
//
//        log.info("//////////////////");
//
//        // AND: la public key PEM estratta con OpenSSL (BEGIN PUBLIC KEY ... END PUBLIC KEY)
//        String openSslPem = """
//        -----BEGIN PUBLIC KEY-----
//        MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAytYrOy71s5KcL8FpSOwC
//        MI/6+ZyaZkjMMbl/BDBtC59hlt8q5CptJihGqaRl5LeLJG7OqMfRteLtpHmsac5r
//        ZmTUncm+mCPMKy1p8EDpYscHneyFGnbbSyH9xKt8QLHV/O8d96dGl/iYNsk7wF8R
//        ihEy62qwfVUgeqhpaVNfEg1FYSOLLbR9OcBKRLamZcJrOqd5vuGNHZKyToqoWqhS
//        ZntbKyZIC93ibnLiQkhetPnrZoCm1s81v8EW6ASbhpWaJEcv3xwe9nZxqjr9tMkO
//        x9sOAT7gIN2hBQZasVxeCelfCZRjyh+P0j37DMpBaPCMlWLUeYQgKrd+aJtyTsnW
//        /QIDAQAB
//        -----END PUBLIC KEY-----
//        """;
//
//        // WHEN: confronto gli encoding SPKI DER
//        byte[] spkiFromOpenSsl = decodePublicKeyPemToDer(openSslPem);
//        byte[] spkiFromCert    = publicKey.getEncoded();
//
//        // THEN: devono coincidere
//        Assertions.assertArrayEquals(spkiFromOpenSsl, spkiFromCert, "SPKI DER must match");
//
//    }



    public static byte[] decodePublicKeyPemToDer(String pem) {
        // Strip header/footer and whitespace, then Base64-decode
        String b64 = pem.replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(b64);
    }

    @Test
    void extractSignaturesFromSignedData() throws IOException, DecoderException, CMSException {
        log.info("TEST extractSignaturesFromSignedData - INIT");
        log.info(" - Leggo il file SOD_HEX e decodifico in byte[] HEX");
        String fileString = Files.readString(basePath.resolve(SOD_IAS_HEX)).replaceAll("\\s+", "");
        String subString = fileString.substring(8);
        byte[] sodIasByteArray = hexFile(subString);
        log.info(" - sodIasByteArray: {}" , Arrays.toString(sodIasByteArray));
        CMSSignedData cms = new CMSSignedData(sodIasByteArray);
        List<byte[]> signatures  = ValidateUtils.extractSignaturesFromSignedData(cms);

        assertFalse(signatures.isEmpty());
        log.info(" - signatures SIZE: {}" , signatures.size());
        //[104, 8, 11, -125, -86, -123, 111, -61, -81, 31, -102, 29, -119, 112, 17, 82, 97, 48, -82, -123, -30, 54, -122, 40, -112, -105, 13, 84, 116, 101, 53, -33, 51, 40, -83, 6, 56, -55, 5, -124, -80, -37, 10, 70, 108, -122, -7, -90, 80, 73, -5, -56, 48, -29, 68, -103, -3, 84, 16, 117, 54, 32, 77, -32, 41, -91, 117, -11, -39, 126, 31, 29, -1, 96, 75, 86, 10, -105, 70, 94, 43, 95, 16, 91, -3, -105, -80, -56, 110, -76, -110, -23, 96, -7, 41, -38, 41, -115, 111, -72, +156 more]
        log.info(" - signatures length: {}" , signatures.get(0).length); //256
        String signaturesToHex = bytesToHex(signatures.get(0));
        log.info("HEX signatures: {}", signaturesToHex);

        CMSSignedData validationDataCms = new CMSSignedData(validationData.getCieIas().getSod());
        List<byte[]> validationDataSignatures  = ValidateUtils.extractSignaturesFromSignedData(cms);
        //Assertions.assertEquals(signaturesToHex, signaturesToVerify.replaceAll("\\s+", ""));
        Assertions.assertTrue(signaturesToHex.equalsIgnoreCase(bytesToHex(validationDataSignatures.get(0))));
    }


    @Test
    void extractFirstAndFiveHashesOctetString() throws IOException, DecoderException, CMSException {

        log.info("TEST extractFirstAndFiveHashesOctetString - INIT");
        log.info(" - Leggo il file SOD_HEX e decodifico in byte[] HEX");

        byte[] sodIasByteArray = hexFile(Files.readString(basePath.resolve(SOD_IAS_HEX)));
        byte[] truncatedSODIAS = Arrays.copyOfRange(sodIasByteArray, 4, sodIasByteArray.length);
        log.info(" - truncatedSODIAS: {}" , Arrays.toString(truncatedSODIAS));
        CMSSignedData cms = new CMSSignedData(truncatedSODIAS);

        // --- PARTE 1: ESTRAI E CALCOLA L'HASH DEI DATI FIRMATI ---
        //la prima occorrenza : #a77dbdb693edd191ec82412c1462c70df7901cdca12088cd23caf74429681a86
        //contiene la concatenazione degli hash dei dati originali ossia i file che sono stati firmati - hashesBlock

        byte[] hashSignedData = ValidateUtils.extractHashBlock(cms);
        String firstStr = calculateDigest(hashSignedData, getFirstDigestAlgorithm(cms));
        Assertions.assertNotNull(firstStr);
        // --- PARTE 2: ESTRAI L'HASH FIRMATO (messageDigest) ---
        SignerInformationStore signers = cms.getSignerInfos();
        if (signers.size() == 0) {
            System.err.println("Nessun firmatario trovato.");
            return;
        }
        ASN1OctetString newFive = ValidateUtils.extractHashSigned(cms);
        log.info("QUINTA OCCORRENZA DI octetString: {}" , newFive.toString());
        Assertions.assertTrue(ValidateUtils.verifyOctetStrings(firstStr, newFive));
    }





    @Test
    void extractSignedAttributes() throws IOException, DecoderException, CMSException {
        log.info("TEST extractSignedAttributes - INIT");
        log.info(" - Leggo il file SOD_HEX e decodifico in byte[] HEX");
        String fileString = Files.readString(basePath.resolve(SOD_IAS_HEX)).replaceAll("\\s+", "");
        String subString = fileString.substring(8);
        byte[] sodIasByteArray = hexFile(subString);
        log.info(" - sodIasByteArray: {}" , Arrays.toString(sodIasByteArray));
        CMSSignedData cms = new CMSSignedData(sodIasByteArray);

        Hashtable<ASN1ObjectIdentifier, Attribute> attributes = ValidateUtils.extractAllSignedAttributes(cms);

        //chiave (OID) della Hashtable
        Enumeration<ASN1ObjectIdentifier> oids = attributes.keys();
        while (oids.hasMoreElements()) {
            ASN1ObjectIdentifier oid = oids.nextElement();
            log.info("  - Attributo (OID): {}" , oid.getId());
            // Ottieni l'oggetto Attribute
            Attribute attribute = attributes.get(oid);
            // Stampa i valori dell'attributo, che sono contenuti in un ASN1Set
            log.info("    Valori:");
            attribute.getAttrValues().forEach(value -> {
                log.info("    - {}" , value.toString());
            });
        }
/*
    signedAttributes.size: 2
    signedAttributes.size: {1.2.840.113549.1.9.3=org.bouncycastle.asn1.cms.Attribute@45079570, 1.2.840.113549.1.9.4=org.bouncycastle.asn1.cms.Attribute@d6ceaeb2}
    - Attributo (OID): 1.2.840.113549.1.9.3
        Valori:
            - 2.23.136.1.1.1
    - Attributo (OID): 1.2.840.113549.1.9.4
        Valori:
            - #c9002855cb7a5d366db2cd6ccd6e148b7f8265e765c520acc88855c2f3338feb
 */
        Assertions.assertNotNull(attributes);

    }

    /*
     * PASSO 1B: ANALISI DEGLI HASH DEI DATI (DataGroupHashes)
     */
// TODO to uncomment when useful test data are available
//    //Verifica specifica dell'hash del NIS...
//    @Test
//    void extractDataGroupHashesForSpecifyNIS() throws IOException, DecoderException, CMSException, NoSuchAlgorithmException {
//        log.info("TEST extractDataGroupHashes - INIT");
//        log.info(" - Leggo il file SOD_HEX e decodifico in byte[] HEX");
//        byte[] sodIasByteArray = hexFile(Files.readString(basePath.resolve(SOD_IAS_HEX)));
//        byte[] truncatedSODIAS = Arrays.copyOfRange(sodIasByteArray, 4, sodIasByteArray.length);
//        log.info(" - sodIasByteArray: {}", Arrays.toString(truncatedSODIAS));
//        CMSSignedData cms = new CMSSignedData(truncatedSODIAS);
//
//        byte[] nisHex = hexFile(cleanString(basePath.resolve("NIS.HEX"))); //m
//        Assertions.assertTrue(ValidateUtils.verifyNisShaFromDataGroup(cms, nisHex));
//
//        List<String> dataGroupList = ValidateUtils.extractDataGroupHashes(cms);
//        // dataGroupList ha 6 elementi
//        Assertions.assertNotNull(dataGroupList);
//
//        //Verifica specifica dell'hash del NIS...
//        String nisHexToCheck = calculateDigest(nisHex, getFirstDigestAlgorithm(cms));
//        //String nisSha256String = decodeNisSha256String(); //D3045B9C6A4104796C408439D38B2C2B6E335FA3CF124F7E3BB4FBCBD2AC473C
//        Assertions.assertTrue(dataGroupList.contains(nisHexToCheck));
//    }

// TODO to uncomment when useful test data are available
//    @Test
//    void extractDataGroupHashesTest() throws Exception {
//        CMSSignedData cms = new CMSSignedData(validationData.getCieIas().getSod());
//        List<String> dataGroups = ValidateUtils.extractDataGroupHashes(cms);
//        String nisHexToCheck = calculateDigest(validationData.getCieIas().getNis(), getFirstDigestAlgorithm(cms));
//
//        boolean found = dataGroups.stream().anyMatch(h -> h.equalsIgnoreCase(nisHexToCheck));
//        Assertions.assertTrue(found);
//    }

    @Test
    void verifyDigitalSignature() throws IOException, DecoderException, CMSException {
        log.info("TEST convertSignedAttributesIntoSet - INIT");
        log.info(" - Leggo il file SOD_HEX e decodifico in byte[] HEX");
        String fileString = Files.readString(basePath.resolve(SOD_IAS_HEX)).replaceAll("\\s+", "");
        String subString = fileString.substring(8);
        byte[] sodIasByteArray = hexFile(subString);
        CMSSignedData cms = new CMSSignedData(sodIasByteArray);

        ResultCieChecker resultCieChecker = ValidateUtils.verifyDigitalSignature(cms);
        assertEquals("OK", resultCieChecker.getValue());
    }

    @Test
    void testDecodeSodHrOk() throws Exception {
        byte[] sodBytes = Files.readAllBytes(sodFile);
        log.info("bytes letti dal file SOD: {} ", sodBytes.length);

        SodSummary summary = decodeSodHr(sodBytes);
        Assertions.assertNotNull(summary);
        Assertions.assertNotNull(summary.getDgDigestAlgorithm());
        Assertions.assertNotNull(summary.getDgExpectedHashes());
        assertFalse(summary.getDgExpectedHashes().isEmpty());
        Assertions.assertNotNull(summary.getSignatureAlgorithm());
        Assertions.assertNotNull(summary.getCmsSignature());
        Assertions.assertNotNull(summary.getDscCertificate());
        log.info("SodSummary: {}", summary);

    }
    @Test
    void testDecodeSodHrFail() {
        byte[] corruptedSod = new byte[]{0x01, 0x02, 0x03}; // SOD corrotto
        log.info("bytes letti dal file SOD corrotto: {} " , corruptedSod.length);

        Exception exception = assertThrows(Exception.class, () -> {
            ValidateUtils.decodeSodHr(corruptedSod);
        });
        log.info("Eccezione catturata: {} - {}", exception.getClass().getSimpleName(), exception.getMessage());
        assertTrue(exception.getMessage().contains("unable") || !exception.getMessage().isEmpty());
    }

    @Test
    @SneakyThrows
    void cleanPublicKeyPadding() {
        CieIas ias = new CieIas();
        ias.setPublicKey(("MIIBCgKCAQEA0o6ic7-TUvAD9rbs4HwIDAQABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .getBytes());
        CieValidationData data = new CieValidationData();
        data.setCieIas(ias);
        assertDoesNotThrow(() -> ValidateUtils.cleanAllCieFields(data));
    }

    @Test
    @SneakyThrows
    void cleanSodIasPadding() {
        CieIas ias = new CieIas();
        String fileString = Files.readString(basePath.resolve(SOD_IAS_PADDING)).replaceAll("\\s+", "");
        String subString = fileString.substring(8);
        byte[] sodIasByteArray = hexFile(subString);
        ias.setSod(sodIasByteArray);
        CieValidationData data = new CieValidationData();
        data.setCieIas(ias);
        assertDoesNotThrow(() -> ValidateUtils.cleanAllCieFields(data));
    }

    @Test
    @SneakyThrows
    void cleanNisPadding(){
        CieIas ias = new CieIas();
        String hexNis = Files.readString(basePath.resolve(NIS_PADDING_FILE_NAME)).trim();
        byte[] nis = hexFile(hexNis);
        ias.setNis(nis);
        CieValidationData data = new CieValidationData();
        data.setCieIas(ias);
        assertDoesNotThrow(() -> ValidateUtils.cleanAllCieFields(data));

    }

    @Test
    @SneakyThrows
    void cleanSodMrtdPadding() {
        CieMrtd mrtd = new CieMrtd();
        String fileString = Files.readString(basePath.resolve(SOD_MRTD_PADDING_HEX)).replaceAll("\\s+", "");
        String subString = fileString.substring(8);
        byte[] sodMrtdByteArray = hexFile(subString);
        mrtd.setSod(sodMrtdByteArray);
        CieValidationData data = new CieValidationData();
        data.setCieMrtd(mrtd);
        assertDoesNotThrow(() -> ValidateUtils.cleanAllCieFields(data));
    }

    @Test
    @SneakyThrows
    void cleanDG1Padding() {
        CieMrtd mrtd = new CieMrtd();
        byte[] dg1 = Files.readAllBytes(dg1Files);
        byte[] dg1Padded = Arrays.copyOf(dg1, dg1.length + 10);
        log.info("DG1 senza padding: {}", HexFormat.of().formatHex(dg1));
        log.info("DG1 con padding: {}", HexFormat.of().formatHex(dg1Padded));
        mrtd.setDg1(dg1Padded);
        CieValidationData data = new CieValidationData();
        data.setCieMrtd(mrtd);
        assertDoesNotThrow(() -> ValidateUtils.cleanAllCieFields(data));

    }

    @Test
    @SneakyThrows
    void cleanDG11Padding() {
        CieMrtd mrtd = new CieMrtd();
        byte[] dg11 = Files.readAllBytes(dg11Files);
        byte[] dg11Padded = Arrays.copyOf(dg11, dg11.length + 10);
        Arrays.fill(dg11Padded, dg11.length, dg11Padded.length, (byte) 0xAA);
        log.info("DG11 senza padding: {}", HexFormat.of().formatHex(dg11));
        log.info("DG11 con padding: {}", HexFormat.of().formatHex(dg11Padded));
        mrtd.setDg11(dg11Padded);
        CieValidationData data = new CieValidationData();
        data.setCieMrtd(mrtd);
        assertDoesNotThrow(() -> ValidateUtils.cleanAllCieFields(data));
    }

    @Test
    @SneakyThrows
    void cleanAllPadding() {
        CieValidationData data = new CieValidationData();
        CieIas ias = new CieIas();
        String fileString = Files.readString(basePath.resolve(SOD_IAS_PADDING)).replaceAll("\\s+", "");
        String subString = fileString.substring(8);
        byte[] sodIasByteArray = hexFile(subString);
        String hexNis = Files.readString(basePath.resolve(NIS_PADDING_FILE_NAME)).trim();
        byte[] nis = hexFile(hexNis);

        ias.setPublicKey(("MIIBCgKCAQEA0o6ic7-TUvAD9rbs4HwIDAQABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .getBytes());
        ias.setSod(sodIasByteArray);
        ias.setNis(nis);

        CieMrtd mrtd = new CieMrtd();
        String fileStringMrtd = Files.readString(basePath.resolve(SOD_MRTD_PADDING_HEX)).replaceAll("\\s+", "");
        String subStringMrtd = fileStringMrtd.substring(8);
        byte[] sodMrtdByteArray = hexFile(subStringMrtd);
        byte[] dg1 = Files.readAllBytes(dg1Files);
        byte[] dg1Padded = Arrays.copyOf(dg1, dg1.length + 10); //solo padding 000
        log.info("DG1 senza padding: {}", HexFormat.of().formatHex(dg1));
        log.info("DG1 con padding: {}", HexFormat.of().formatHex(dg1Padded));
        byte[] dg11 = Files.readAllBytes(dg11Files);
        byte[] dg11Padded = Arrays.copyOf(dg11, dg11.length + 10);
        Arrays.fill(dg11Padded, dg11.length, dg11Padded.length, (byte) 0xAA); //padding "misto"
        log.info("DG11 senza padding: {}", HexFormat.of().formatHex(dg11));
        log.info("DG11 con padding: {}", HexFormat.of().formatHex(dg11Padded));

        mrtd.setSod(sodMrtdByteArray);
        mrtd.setDg1(dg1Padded);
        mrtd.setDg11(dg11Padded);

        data.setCieIas(ias);
        data.setCieMrtd(mrtd);
        assertDoesNotThrow(() -> ValidateUtils.cleanAllCieFields(data));

    }


    private static byte[] hexFile(String toHex) throws DecoderException {
        return Hex.decodeHex(toHex);
    }

    public static List<byte[]> pickManyDerFromResources(int n) throws Exception {
        List<Path> ders = listDerFiles();
        Assertions.assertTrue(ders.size() >= n);
        List<byte[]> out = new ArrayList<>();
        if ( n == -1 ){
            for (Path der : ders) {
                out.add(Files.readAllBytes(der));
            }
        } else {
            for (int i = 0; i < n; i++) out.add(Files.readAllBytes(ders.get(i)));
        }
        return out;
    }

    public static byte[] toPem(byte[] der) {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der);
        String pem = "-----BEGIN CERTIFICATE-----\n" + b64 + "\n-----END CERTIFICATE-----\n";
        return pem.getBytes(StandardCharsets.US_ASCII);
    }

    private static List<Path> listDerFiles() throws Exception {
        try (var s = Files.list(CSCA_DIR)) {
            return s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cer"))
                    .collect(Collectors.toList());
        }
    }


    public boolean verifyNisPublicKeyFromDataGroup(CMSSignedData cmsData, byte[] nisSha256PublicKey) throws CieCheckerException, NoSuchAlgorithmException {
        if (cmsData == null || nisSha256PublicKey == null) {
            throw new CieCheckerException("Input parameters NULL: CMSSignedData is " + cmsData + " - String is " + nisSha256PublicKey);
        }
        String nisSha256PublicKeyToCheck = calculateSha256(nisSha256PublicKey);
        List<String> dataGroupList = ValidateUtils.extractDataGroupHashes(cmsData);
        if(dataGroupList.contains(nisSha256PublicKeyToCheck)){
            return true;
        }else{
            log.error("The dataGroupList do not contains nisSha256PublicKey");
            return false;
        }
    }


    private static byte[] loadSodBytes(Path filePath) throws IOException, DecoderException {
        String fileString = Files.readString(filePath).replaceAll("\\s+", "");
        return hexFile(fileString.substring(8));
    }

    private static String cleanString(Path file) throws IOException {
        return Files.readString(file).replaceAll("\\s+", "");
    }

    public static List<X509Certificate> parseCscaAnchors(Collection<byte[]> cscaAnchorBlobs,
                                                         CertificateFactory x509Cf) throws CieCheckerException {
        if (cscaAnchorBlobs == null || cscaAnchorBlobs.isEmpty()) {
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_CSCA_ANCHORS_PROVIDED);
        }
        try {
            List<X509Certificate> out = new ArrayList<>();
            for (byte[] blob : cscaAnchorBlobs) {
                Collection<? extends Certificate> cs = x509Cf.generateCertificates(new ByteArrayInputStream(blob));
                for (Certificate c : cs) out.add((X509Certificate) c);
            }
            if (out.isEmpty()) throw new CieCheckerException(ResultCieChecker.KO_EXC_PARSED_ZERO_CSCA_CERTIFICATES);
            return out;

        }catch (CertificateException ce){
            log.error("Error in parseCscaAnchors: {}", ce.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_GENERATE_CERTIFICATE, ce);
        }
    }

    private String calculateSha256(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(
                bytes);
        return bytesToHex(encodedhash);
    }


}
