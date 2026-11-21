package it.pagopa.pn.ciechecker.generator.loader;

import it.pagopa.pn.ciechecker.generator.model.CertAndKey;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static it.pagopa.pn.ciechecker.utils.ValidateUtils.*;

@Slf4j
public class CertAndKeyLoader {

    S3Client s3;


    public CertAndKeyLoader() {
        this.s3 = S3Client.builder().build();
    }

    public CertAndKey loadIssuerCertAndKeyFromS3() throws IOException, GeneralSecurityException {
        String bucket = System.getProperty("cie.generator.bucket");
        String key = System.getProperty("cie.generator.file-key");
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try (InputStream s3Input = s3.getObject(request);
             ZipInputStream zis = new ZipInputStream(s3Input)) {
            return extractCertificateFromZip(zis);
        }
    }

    public CertAndKey extractCertificateFromZip(ZipInputStream zis) throws IOException, GeneralSecurityException {

        X509Certificate certificate = null;
        PrivateKey privateKey = null;
        for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
            if (zipEntry.isDirectory()) continue;

            String name = zipEntry.getName();
            byte[] entryBytes = readAll(zis);
            zis.closeEntry();

            if (name.endsWith(".pem")) {
                certificate = parseCertificate(entryBytes);
            } else if (name.endsWith(".key")) {
                privateKey = parsePrivateKey(entryBytes);
            }
        }

        if (certificate == null) {
            throw new FileNotFoundException(".pem non trovato nello zip");
        }
        if (privateKey == null) {
            throw new FileNotFoundException(".key non trovato nello zip");
        }
        return new CertAndKey(certificate, new KeyPair(certificate.getPublicKey(), privateKey));

    }



}
