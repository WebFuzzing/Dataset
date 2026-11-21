package it.pagopa.pn.ciechecker.generator.pki;

import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.generator.model.CertAndKey;
import it.pagopa.pn.ciechecker.generator.model.Issuer;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

import static it.pagopa.pn.ciechecker.utils.CieCheckerConstants.*;

public class CiePki {

    static final String SIGNATURE_ALGORITHM = SHA_256_WITH_RSA;
    public static final int RSA_BITS_IAS = 2048;


    public KeyPair generateRsaKeyPair(int bits) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(bits);
        return keyGen.generateKeyPair();
    }

    public Issuer issuerFrom(X509Certificate caCert, PrivateKey caKey) {
        Objects.requireNonNull(caCert, "CA cert null");
        Objects.requireNonNull(caKey, "CA key null");
        return new Issuer(caCert, caKey);
    }

    public CertAndKey issueDocumentSigner(String subjectDn,
                                          Issuer issuer,
                                          int keySize,
                                          int validityDays)
            throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {

        Objects.requireNonNull(issuer, "Issuer null");
        Objects.requireNonNull(issuer.certificate(), EXC_DOC_SIGNER_CERT_NULL);
        Objects.requireNonNull(issuer.privateKey(), EXC_DOC_SIGNER_KEY_NULL);

        KeyPair kp = generateRsaKeyPair(keySize);

        X500Name issuerName  = new JcaX509CertificateHolder(issuer.certificate()).getSubject();
        X500Name subjectName = new X500Name(subjectDn);

        Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
        Date notBefore = Date.from(now);
        Date notAfter  = Date.from(now.plus(validityDays, ChronoUnit.DAYS));

        ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(issuer.privateKey());

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerName,
                new BigInteger(128, new SecureRandom()),
                notBefore, notAfter,
                subjectName,
                kp.getPublic()
        );

        JcaX509ExtensionUtils ext = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        builder.addExtension(Extension.subjectKeyIdentifier, false, ext.createSubjectKeyIdentifier(kp.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false, ext.createAuthorityKeyIdentifier(issuer.certificate()));

        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        return new CertAndKey(cert, kp);
    }

    public CertAndKey issueDocumentSigner(Issuer issuer,
                                          int keySize,
                                          int validityDays)
            throws NoSuchAlgorithmException, CertificateException, CertIOException, OperatorCreationException {
        String subjectDn=issuer.certificate().getIssuerX500Principal().getName();
        return issueDocumentSigner(subjectDn,issuer,keySize,validityDays);
    }

    public CertAndKey issueIasCertificate(String subjectDn,
                                          X509Certificate dsCert,
                                          PrivateKey dsPrivateKey,
                                          int validityDays) throws NoSuchAlgorithmException {
        Objects.requireNonNull(dsCert, EXC_DOC_SIGNER_CERT_NULL);
        Objects.requireNonNull(dsPrivateKey, EXC_DOC_SIGNER_KEY_NULL);

        KeyPair iasKeys = generateRsaKeyPair(RSA_BITS_IAS);
        X509Certificate iasCert = issueEndEntityCert(subjectDn, iasKeys.getPublic(), dsCert, dsPrivateKey, validityDays);
        return new CertAndKey(iasCert, iasKeys);
    }

    public byte[] signWithIasPrivate(byte[] data, PrivateKey iasPrivate) {
        try {
            Cipher c = Cipher.getInstance(RSA_ECB_PKCS1_PADDING);
            c.init(Cipher.ENCRYPT_MODE, iasPrivate);
            return c.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(EXC_IAS_SIGN_FAILED, e);
        }
    }

    private X509Certificate issueEndEntityCert(String subjectDn,
                                               PublicKey subjectPublicKey,
                                               X509Certificate issuerCert,
                                               PrivateKey issuerKey,
                                               int validityDays) {
        try {
            Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
            Date notBefore = Date.from(now);
            Date notAfter  = Date.from(now.plus(validityDays, ChronoUnit.DAYS));
            BigInteger serial = new BigInteger(128, new SecureRandom());

            X500Name subject = new X500Name(subjectDn);
            X500Name issuer  = new X500Name(issuerCert.getSubjectX500Principal().getName());

            JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(
                    issuer, serial, notBefore, notAfter, subject, subjectPublicKey);

            JcaX509ExtensionUtils ext = new JcaX509ExtensionUtils();
            b.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
            b.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            b.addExtension(Extension.subjectKeyIdentifier, false, ext.createSubjectKeyIdentifier(subjectPublicKey));
            b.addExtension(Extension.authorityKeyIdentifier, false, ext.createAuthorityKeyIdentifier(issuerCert));

            ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(issuerKey);
            X509CertificateHolder holder = b.build(signer);
            return new JcaX509CertificateConverter().getCertificate(holder);
        } catch (Exception e) {
            throw new IllegalStateException(EXC_CERT_ISSUANCE_FAILED, e);
        }
    }

    public byte[] toDer(X509Certificate cert) {
        try {
            return cert.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(EXC_DER_ENCODING_FAILED, e);
        }
    }

    public byte[] toPem(X509Certificate cert) {
        try {
            String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(cert.getEncoded());
            String pem = CERTIFICATE_START + b64 + CERTIFICATE_END;
            return pem.getBytes(StandardCharsets.US_ASCII);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException(EXC_PEM_ENCODING_FAILED, e);
        }
    }


    public CertAndKey createDevTestCA(String dn, int keyBits, int validityDays){
        try {
            KeyPair caKeys = generateRsaKeyPair(keyBits);
            X509Certificate ca = selfSign(dn, caKeys, validityDays);
            return new CertAndKey(ca, caKeys);
        } catch (NoSuchAlgorithmException e) {
            throw new CieCheckerException(e);
        }
    }

    private X509Certificate selfSign(String subj, KeyPair keyPair, int validityDays)  {
        try {
            Instant now = Instant.now();
            Date notBefore = Date.from(now.minus(1, ChronoUnit.DAYS));
            Date notAfter  = Date.from(now.plus(validityDays, ChronoUnit.DAYS));
            BigInteger serial = new BigInteger(128, new SecureRandom());

            X500Name subject = new X500Name(subj);
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

            JcaX509ExtensionUtils ext = new JcaX509ExtensionUtils();
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
            builder.addExtension(Extension.subjectKeyIdentifier, false, ext.createSubjectKeyIdentifier(keyPair.getPublic()));
            builder.addExtension(Extension.authorityKeyIdentifier, false, ext.createAuthorityKeyIdentifier(keyPair.getPublic()));

            ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(keyPair.getPrivate());
            X509CertificateHolder holder = builder.build(signer);
            return new JcaX509CertificateConverter().getCertificate(holder);
        } catch (RuntimeException | CertificateException | OperatorCreationException | NoSuchAlgorithmException |
                 CertIOException e) {
            throw new CieCheckerException(e);
        }
    }
}
