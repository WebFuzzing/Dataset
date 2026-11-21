package it.pagopa.pn.ciechecker.generator.pki;

import it.pagopa.pn.ciechecker.generator.model.CertAndKey;
import it.pagopa.pn.ciechecker.generator.model.Issuer;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CiePkiTest {


    static CiePki pki;

    @BeforeAll
    static void init() {
        pki = new CiePki();
    }

    @Test
    void createChainAndVerityTest() throws Exception {
        CertAndKey ca = pki.createDevTestCA("CN=Dev Test CA,O=PagoPA,L=RM,C=IT", 3072, 3650);
        ca.getCertificate().checkValidity();

        Issuer issuer = new Issuer(ca.getCertificate(), ca.getPrivateKey());
        CertAndKey ds = pki.issueDocumentSigner("CN=DocumentSigner-DEV,O=PagoPA,L=RM,C=IT",
                issuer, 2048, 1825);
        ds.getCertificate().checkValidity();

        CertAndKey ias = pki.issueIasCertificate("CN=CIE-IAS,O=PagoPA,L=RM,C=IT",
                ds.getCertificate(), ds.getPrivateKey(), 1095);
        ias.getCertificate().checkValidity();

        ds.getCertificate().verify(ca.getPublicKey());
        ias.getCertificate().verify(ds.getPublicKey());
    }

    @Test
    void exportToDerTest() throws Exception {
        CertAndKey ca = pki.createDevTestCA("CN=RoundTripCA,O=PagoPA,C=IT", 3072, 3650);

        byte[] der = pki.toDer(ca.getCertificate());
        byte[] pem = pki.toPem(ca.getCertificate());

        // DER → X509
        X509Certificate xDer = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(der));

        // PEM → DER → X509
        String pemStr = new String(pem);
        String b64 = pemStr.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
        byte[] derFromPem = Base64.getDecoder().decode(b64);
        X509Certificate xPem = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(derFromPem));

        assertEquals(ca.getCertificate().getSubjectX500Principal(), xDer.getSubjectX500Principal());
        assertEquals(ca.getCertificate().getSubjectX500Principal(), xPem.getSubjectX500Principal());
        assertArrayEquals(der, derFromPem, "DER originario e DER estratto dal PEM devono coincidere");
    }

    @Test
    void signNonceDecryptAndVerifyTest() throws Exception {
        CertAndKey ca = pki.createDevTestCA("CN=Dev CA,O=PagoPA,C=IT", 3072, 3650);
        Issuer issuer = new Issuer(ca.getCertificate(), ca.getPrivateKey());
        CertAndKey ds = pki.issueDocumentSigner("CN=DS,O=PagoPA,C=IT", issuer, 2048, 1825);
        CertAndKey ias = pki.issueIasCertificate("CN=IAS,O=PagoPA,C=IT",
                ds.getCertificate(), ds.getPrivateKey(), 1095);

        byte[] nonce = "hello-nonce".getBytes();
        byte[] signed = pki.signWithIasPrivate(nonce, ias.getPrivateKey());

        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.DECRYPT_MODE, ias.getPublicKey());
        byte[] plain = c.doFinal(signed);

        assertArrayEquals(nonce, plain);
    }

    @Test
    void certificatesValidityTest() throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, CertIOException {
        CertAndKey ca = pki.createDevTestCA("CN=ValidityCA,O=PagoPA,C=IT", 3072, 3650);
        Issuer issuer = new Issuer(ca.getCertificate(), ca.getPrivateKey());
        CertAndKey ds = pki.issueDocumentSigner("CN=DS,O=PagoPA,C=IT", issuer, 2048, 1825);
        CertAndKey ias = pki.issueIasCertificate("CN=IAS,O=PagoPA,C=IT",
                ds.getCertificate(), ds.getPrivateKey(), 1095);

        Instant now = Instant.now();
        assertTrue(ca.getCertificate().getNotBefore().toInstant().isBefore(now.plusSeconds(3600)));
        assertTrue(ds.getCertificate().getNotAfter().toInstant().isAfter(now.plusSeconds(24 * 3600)));
        assertTrue(ias.getCertificate().getNotAfter().toInstant().isAfter(now.plusSeconds(24 * 3600)));
    }

    @Test
    void wrongIssuerDoesNotVerifyDs() throws CertificateException, NoSuchAlgorithmException, OperatorCreationException, CertIOException {
        CertAndKey ca1 = pki.createDevTestCA("CN=CA1,O=PagoPA,C=IT", 3072, 3650);
        assertTrue(ca1.getCertificate().getNotBefore().toInstant().isBefore(Instant.now()));
        assertTrue(ca1.getCertificate().getNotAfter().toInstant().isAfter(Instant.now()));
        Issuer issuer1 = new Issuer(ca1.getCertificate(), ca1.getPrivateKey());
        CertAndKey ds = pki.issueDocumentSigner("CN=DS,O=PagoPA,C=IT", issuer1, 2048, 1825);

        CertAndKey ca2 = pki.createDevTestCA("CN=CA2,O=PagoPA,C=IT", 3072, 3650);
        assertThrows(Exception.class, () -> ds.getCertificate().verify(ca2.getPublicKey()));
    }
}
