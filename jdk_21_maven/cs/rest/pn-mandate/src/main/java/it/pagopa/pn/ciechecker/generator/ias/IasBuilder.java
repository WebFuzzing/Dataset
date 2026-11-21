package it.pagopa.pn.ciechecker.generator.ias;


import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.model.CieIas;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.ciechecker.utils.LogsConstant;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.*;
import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSTypedData;

import java.util.Map;

@Slf4j
public class IasBuilder {

    public static final int DEFAULT_NIS_LEN = 12;
    private final SecureRandom rng;
    private static final String DIGITS = "0123456789";

    public IasBuilder() {
        this.rng = new SecureRandom();
    }

    public IasBuilder(SecureRandom rng) {
        this.rng = rng;
    }

    public String generateNisNumericString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be > 0");
        }
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }

    public byte[] generateNisRandomBytes(int length) {
        if (length <= 0 || length > 64) {
            throw new IllegalArgumentException("NIS length must be 1..64");
        }
        byte[] out = new byte[length];
        rng.nextBytes(out);
        return out;
    }

    public byte[] generateFromSeed(String... parts) {
        return generateFromSeed(DEFAULT_NIS_LEN, parts);
    }

    public byte[] generateFromSeed(int length, String... parts) {
        if (length <= 0 || length > 64) {
            throw new IllegalArgumentException("NIS length must be 1..64");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String p : parts) {
                if (p != null) {
                    md.update(p.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
                    md.update((byte) 0x00); // separatore
                }
            }
            byte[] hash = md.digest();
            return Arrays.copyOf(hash, length);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute seeded NIS", e);
        }
    }

    public static boolean isValid(byte[] nis) {
        return nis != null && nis.length >= 8 && nis.length <= 64;
    }

    public static ByteBuffer asReadOnlyBuffer(byte[] nis) {
        return ByteBuffer.wrap(nis).asReadOnlyBuffer();
    }


    public static CieIas createCieIas(byte[] nis, byte[] iasPublicKeyDer,
                                      PrivateKey dsKey, X509Certificate caCert) throws CieCheckerException {
        try{
            CieIas cieIas = new CieIas();
            cieIas.setNis(nis);
            cieIas.setSod(buildIasSodWithDocumentSigner(nis, iasPublicKeyDer, caCert, dsKey));
            cieIas.setPublicKey(iasPublicKeyDer);
            return cieIas;
        }catch (Exception e ){
            log.error(Exception.class + LogsConstant.MESSAGE  + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

    public static byte[] buildIasSodWithDocumentSigner(
            byte[] nis,
            byte[] iasPublicKeyDer,
            X509Certificate cscaCert, PrivateKey privateKey
    ) throws CieCheckerException {

        try{
            Map<Integer, byte[]> dgHashMap = new LinkedHashMap<>();
            MessageDigest sha256 = MessageDigest.getInstance("SHA-512");
            dgHashMap.put(1, sha256.digest(nis));
            dgHashMap.put(5, sha256.digest(iasPublicKeyDer));
            return buildSignedSod(dgHashMap, privateKey, cscaCert);
        }catch (Exception e ){
            log.error(Exception.class + LogsConstant.MESSAGE  + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

    public static byte[] buildSignedSod(Map<Integer, byte[]> dgHashMap,
                                        PrivateKey dsKey, X509Certificate dsCert) throws CieCheckerException {

        try {
            // DataGroupHash sequence
            ASN1EncodableVector dgHashVector = new ASN1EncodableVector();
            for (Map.Entry<Integer, byte[]> entry : dgHashMap.entrySet()) {
                ASN1EncodableVector dgEntry = new ASN1EncodableVector();
                dgEntry.add(new ASN1Integer(entry.getKey()));
                dgEntry.add(new DEROctetString(entry.getValue()));
                dgHashVector.add(new DERSequence(dgEntry));
            }

            DataGroupHash[] dghArray = dgHashMap.entrySet().stream()
                    .map(e -> new DataGroupHash(e.getKey(), new DEROctetString(e.getValue())))
                    .toArray(DataGroupHash[]::new);
            //LDSSecurityObject
            LDSSecurityObject ldsSO = new LDSSecurityObject(
                    new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256),
                    dghArray
            );

            // Encode LDSSecurityObject
            byte[] ldsSOBytes = ldsSO.getEncoded("DER");
            CMSTypedData cmsData = new CMSProcessableByteArray(CMSObjectIdentifiers.data, ldsSOBytes);

            // CMS Signature
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA512withRSA")
                    .setProvider(new BouncyCastleProvider()).build(dsKey);

            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(
                    new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder().setProvider(new BouncyCastleProvider()).build()
                    ).build(contentSigner, dsCert)
            );

            generator.addCertificates(new JcaCertStore(Collections.singletonList(dsCert)));

            CMSSignedData signedData = generator.generate(cmsData, true);

            byte[] sod = new byte[signedData.getEncoded().length + 4];
            System.arraycopy(signedData.getEncoded(), 0, sod, 4, signedData.getEncoded().length);
            return sod;
        }catch (Exception e ){
            log.error(Exception.class + LogsConstant.MESSAGE  + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

}
