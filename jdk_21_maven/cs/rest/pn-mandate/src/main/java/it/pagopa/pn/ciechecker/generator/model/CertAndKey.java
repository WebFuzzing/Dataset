package it.pagopa.pn.ciechecker.generator.model;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class CertAndKey {

    X509Certificate certificate;
    KeyPair keyPair;

    public CertAndKey(X509Certificate certificate, KeyPair keyPair) {
        this.certificate = certificate;
        this.keyPair = keyPair;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPublicKey() {
        return certificate.getPublicKey();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public byte[] getEncodedCertificate() throws CertificateEncodingException {
        return certificate.getEncoded();
    }

    public byte[] getEncodedPrivateKey(){
        return keyPair.getPrivate().getEncoded();
    }

    public byte[] getEncodedPublicKey() {
        return keyPair.getPublic().getEncoded();
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }
}