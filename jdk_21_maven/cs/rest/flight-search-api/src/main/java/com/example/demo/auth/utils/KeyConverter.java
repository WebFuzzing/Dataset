package com.example.demo.auth.utils;

import lombok.experimental.UtilityClass;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Utility class for converting PEM-encoded public and private keys to {@link PublicKey} and {@link PrivateKey} objects.
 * This class provides methods to convert PEM-encoded key strings to their respective {@link PublicKey} and {@link PrivateKey} objects
 * using the BouncyCastle library.
 */
@UtilityClass
public class KeyConverter {

    /**
     * Converts a PEM-encoded public key string to a {@link PublicKey} object.
     * This method parses a PEM-encoded public key string, extracts the public key information, and returns the corresponding
     * {@link PublicKey} object.
     *
     * @param publicPemKey The PEM-encoded public key string.
     * @return The corresponding {@link PublicKey} object.
     * @throws RuntimeException if there is an error reading or parsing the PEM key.
     */
    public PublicKey convertPublicKey(final String publicPemKey) {

        final StringReader keyReader = new StringReader(publicPemKey);
        try {
            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo
                    .getInstance(new PEMParser(keyReader).readObject());
            return new JcaPEMKeyConverter().getPublicKey(publicKeyInfo);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

    /**
     * Converts a PEM-encoded private key string to a {@link PrivateKey} object.
     * This method parses a PEM-encoded private key string, extracts the private key information, and returns the corresponding
     * {@link PrivateKey} object.
     *
     * @param privatePemKey The PEM-encoded private key string.
     * @return The corresponding {@link PrivateKey} object.
     * @throws RuntimeException if there is an error reading or parsing the PEM key.
     */
    public PrivateKey convertPrivateKey(final String privatePemKey) {

        StringReader keyReader = new StringReader(privatePemKey);
        try {
            PrivateKeyInfo privateKeyInfo = PrivateKeyInfo
                    .getInstance(new PEMParser(keyReader).readObject());
            return new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

    }

}
