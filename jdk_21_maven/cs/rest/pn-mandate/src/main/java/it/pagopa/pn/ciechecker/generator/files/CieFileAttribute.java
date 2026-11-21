package it.pagopa.pn.ciechecker.generator.files;

import lombok.Getter;

public enum CieFileAttribute {

    CHALLANGE("validationData.signedNonce", "challenge/signedNonce.bin"),
    // Attributi CIE-IAS
    IAS_SOD("validationData.cieIas.sod", "ias/EF.SOD.der"),
    IAS_NIS("validationData.cieIas.nis", "ias/EF.ID_Servizi.bin"),
    IAS_PUBLIC_KEY("validationData.cieIas.publicKey", "ias/EF.CIE.Kpub.der"),
    IAS_CERT_CIE("cieCaAndKey.certPem", "ias/EF.Cert_CIE.der"),

    // Attributi CIE-MRTD
    MRTD_SOD("validationData.cieMrtd.sod", "mrtd/EF.SOD.der"),
    MRTD_DG1("validationData.cieMrtd.dg1", "mrtd/EF.DG1.bin"),
    MRTD_DG11("validationData.cieMrtd.dg11", "mrtd/EF.DG11.bin"),

    PKI_CERT_PEM("cieCaAndKey.certPem", "pki/Certificato.pem"),
    PKI_CERT_K("cieCaAndKey.certKey", "pki/Certificato.K");

    @Getter
    private final String attributePath;
    @Getter
    private final String relativeFilePath;

    CieFileAttribute(String attributePath, String relativeFilePath) {
        this.attributePath = attributePath;
        this.relativeFilePath = relativeFilePath;
    }
}
