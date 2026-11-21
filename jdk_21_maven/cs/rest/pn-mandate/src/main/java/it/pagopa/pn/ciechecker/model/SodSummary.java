package it.pagopa.pn.ciechecker.model;

import lombok.Data;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.util.encoders.Hex;

import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * /**
 * Rappresenta il riassunto del contenuto del file EF.SOD
 * Contiene:
 * - OID del contenuto firmato (es. pkcs7-signedData)
 * - Algoritmo di digest usato per i DataGroup
 * - Mappa dei digest attesi (DG -> hash)
 * - Algoritmo di firma
 * - Firma digitale
 * - Certificato DSC (Document Signer Certificate)
 * è l’equivalente del riassunto che produce lo script `decode_sod_hr.sh`
 */
@Data
public final class SodSummary {
    private final String contentTypeOid;
    private final AlgorithmIdentifier dgDigestAlgorithm;
    private final Map<Integer, byte[]> dgExpectedHashes; // preserves order
    private final AlgorithmIdentifier signatureAlgorithm;
    private final byte[] cmsSignature;
    private final X509Certificate dscCertificate;

    public SodSummary(String contentTypeOid,
                      AlgorithmIdentifier dgDigestAlgorithm,
                      Map<Integer, byte[]> dgExpectedHashes,
                      AlgorithmIdentifier signatureAlgorithm,
                      byte[] cmsSignature,
                      X509Certificate dscCertificate) {
        this.contentTypeOid = contentTypeOid;
        this.dgDigestAlgorithm = dgDigestAlgorithm;
        this.dgExpectedHashes = dgExpectedHashes;
        this.signatureAlgorithm = signatureAlgorithm;
        this.cmsSignature = cmsSignature;
        this.dscCertificate = dscCertificate;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ContentType OID: ").append(contentTypeOid).append('\n');
        sb.append("DG Digest Alg: ").append(dgDigestAlgorithm.getAlgorithm().getId())
                .append(" (").append(dgDigestAlgorithm.getAlgorithm()).append(")\n");
        sb.append("DG expected hashes:\n");
        dgExpectedHashes.forEach((dg,h) -> sb.append(String.format("  DG%-2d -> %s%n", dg, Hex.toHexString(h))));
        sb.append("Signature Alg OID: ").append(signatureAlgorithm.getAlgorithm().getId()).append('\n');
        sb.append("Signature (hex, head): ");
        String sigHex = Hex.toHexString(cmsSignature);
        sb.append(sigHex.substring(0, Math.min(64, sigHex.length()))).append("...\n");
        if (dscCertificate != null) {
            sb.append("DSC Subject: ").append(dscCertificate.getSubjectX500Principal()).append('\n');
            sb.append("DSC Issuer : ").append(dscCertificate.getIssuerX500Principal()).append('\n');
            sb.append("DSC Serial : ").append(dscCertificate.getSerialNumber()).append('\n');
        }
        return sb.toString();
    }
}

