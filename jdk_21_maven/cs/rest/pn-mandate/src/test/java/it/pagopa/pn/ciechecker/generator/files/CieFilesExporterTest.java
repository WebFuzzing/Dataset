package it.pagopa.pn.ciechecker.generator.files;

import it.pagopa.pn.ciechecker.generator.model.CieCaAndKey;
import it.pagopa.pn.ciechecker.model.CieIas;
import it.pagopa.pn.ciechecker.model.CieMrtd;
import it.pagopa.pn.ciechecker.model.CieValidationData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class CieFilesExporterTest {

    @Test
    void generatorFileCieTEST() {

        // Dati fittizi
        byte[] sodIasBytes = "IAS_SOD_DATA".getBytes();
        byte[] pubKeyIasBytes = "IAS_PUBLICKEY_DATA".getBytes();
        byte[] nisIasBytes = "IAS_NIS_DATA".getBytes();

        byte[] sodMrtdBytes = "MRTD_SOD_DATA".getBytes();
        byte[] dg1Bytes = "DG1_DATA".getBytes();
        byte[] dg2Bytes = "DG11_DATA".getBytes();

        byte[] signedNonce = "SIGNED_NONCE_DATA".getBytes();

        byte[] certPem = "CERT_PEM_DATA".getBytes();
        byte[] certKey = "CERT_KEY_DATA".getBytes();

        CieIas cieIas = new CieIas();
        cieIas.setSod(sodIasBytes);
        cieIas.setPublicKey(pubKeyIasBytes);
        cieIas.setNis(nisIasBytes);

        CieMrtd cieMrtd = new CieMrtd();
        cieMrtd.setSod(sodMrtdBytes);
        cieMrtd.setDg1(dg1Bytes);
        cieMrtd.setDg11(dg2Bytes);

        CieValidationData data = new CieValidationData();
        data.setCieIas(cieIas);
        data.setCieMrtd(cieMrtd);
        data.setSignedNonce(signedNonce);
        data.setNonce("02461"); // nisChallenge);
        data.setCodFiscDelegante("RSSMRA95A58H501Z");


        // Directory di base per il salvataggio
        String outputDir = "src/test/resources/output-dir";
        CieCaAndKey cieCaAndKey = new CieCaAndKey();
        cieCaAndKey.setCertPem(certPem);
        cieCaAndKey.setCertKey(certKey);

        CieFilesExporter generator = new CieFilesExporter(data, cieCaAndKey, outputDir);
        Map<String, Long> results = generator.exportCieArtifactsToFiles();
        Assertions.assertTrue(results.size() >= 7);
    }
}
