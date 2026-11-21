package it.pagopa.pn.ciechecker.generator.api;

import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.generator.challenge.ChallengeResponseBuilder;
import it.pagopa.pn.ciechecker.generator.constants.CieGeneratorConstants;
import it.pagopa.pn.ciechecker.generator.files.CieFilesExporter;
import it.pagopa.pn.ciechecker.generator.ias.IasBuilder;
import it.pagopa.pn.ciechecker.generator.loader.CertAndKeyLoader;
import it.pagopa.pn.ciechecker.generator.model.CertAndKey;
import it.pagopa.pn.ciechecker.generator.model.CieCaAndKey;
import it.pagopa.pn.ciechecker.generator.model.Issuer;
import it.pagopa.pn.ciechecker.generator.pki.CiePki;
import it.pagopa.pn.ciechecker.generator.sod.SodMrtdBuilder;
import it.pagopa.pn.ciechecker.model.CieIas;
import it.pagopa.pn.ciechecker.model.CieMrtd;
import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.ciechecker.utils.LogsConstant;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;


@Slf4j
public class CieGeneratorApiImpl implements CieGeneratorApi {

    CertAndKeyLoader certAndKeyLoader = new CertAndKeyLoader();


    @Override
    public CieValidationData generateCieValidationData(Path outputDir,
                                                       String codiceFiscaleDelegante,
                                                       String codiceFiscaleCIE,
                                                       LocalDate expirationDate,
                                                       String nonce) throws CieCheckerException {

        try {
            CiePki pki = new CiePki();

            //recupero cert e key
            CertAndKey issuerCertAndKeyFromS3 = certAndKeyLoader.loadIssuerCertAndKeyFromS3();


            //costruisco document signer
            CertAndKey userCertificate = pki.issueDocumentSigner(
                    new Issuer(issuerCertAndKeyFromS3.getCertificate(), issuerCertAndKeyFromS3.getPrivateKey()),
                    2048,
                    365
            );


            IasBuilder iasBuilder = new IasBuilder();
            // creazione ias
            CieIas ias = iasBuilder.createCieIas(
                    iasBuilder.generateNisNumericString(IasBuilder.DEFAULT_NIS_LEN).getBytes(),     //NIS
                    userCertificate.getEncodedPublicKey(),                                   //PUBKEY
                    issuerCertAndKeyFromS3.getPrivateKey(),                                         //PRVKEY
                    issuerCertAndKeyFromS3.getCertificate()                                         //CERT
            );

            CieMrtd mrtd = new SodMrtdBuilder().buildCieMrtdAndSignSodWithDocumentSigner(
                    CieGeneratorConstants.DEFAULT_SURNAME,
                    CieGeneratorConstants.DEFAULT_GIVEN_NAME,
                    CieGeneratorConstants.DEFAULT_DOCUMENT_NUMBER,
                    CieGeneratorConstants.DEFAULT_NATIONALITY,
                    CieGeneratorConstants.DEFAULT_DATE_OF_BIRTH,
                    CieGeneratorConstants.DEFAULT_SEX,
                    expirationDate,
                    codiceFiscaleCIE,
                    CieGeneratorConstants.DEFAULT_PLACE_OF_BIRTH,
                    issuerCertAndKeyFromS3.getPrivateKey(),
                    issuerCertAndKeyFromS3.getCertificate()
            );


            CieValidationData validationData = new CieValidationData();


            validationData.setCieMrtd(mrtd);
            validationData.setCieIas(ias);
            validationData.setNonce(nonce);
            validationData.setCodFiscDelegante(codiceFiscaleDelegante);

            validationData.setSignedNonce(ChallengeResponseBuilder.signNonce(
                    nonce,
                    userCertificate.getPrivateKey())
            );

            // CONVERT TO CIECAANDKEY
            CieCaAndKey cieCaAndkey = new CieCaAndKey();
            cieCaAndkey.setCertPem(userCertificate.getEncodedCertificate());
            cieCaAndkey.setCertKey(userCertificate.getEncodedPrivateKey());
            //

            //EXPORT FILES
            final CieFilesExporter generator = new CieFilesExporter(
                    validationData,
                    cieCaAndkey,
                    outputDir.toAbsolutePath().toString()
            );

            final Set<String> exportedKeys = generator.exportCieArtifactsToFiles().keySet();
            exportedKeys.forEach(key ->
                    log.debug("Exported file: {} ", key)
            );
            //
            return validationData;
        } catch (Exception e) {
            log.error(Exception.class + LogsConstant.MESSAGE + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }
}
