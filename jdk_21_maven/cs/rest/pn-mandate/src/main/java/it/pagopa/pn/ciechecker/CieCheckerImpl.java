package it.pagopa.pn.ciechecker;

import it.pagopa.pn.ciechecker.client.s3.S3BucketClient;
import it.pagopa.pn.ciechecker.utils.CieCheckerConstants;
import it.pagopa.pn.ciechecker.utils.LogsConstant;
import it.pagopa.pn.ciechecker.utils.ValidateUtils;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import lombok.*;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import static it.pagopa.pn.ciechecker.utils.CieCheckerConstants.*;
import it.pagopa.pn.ciechecker.model.*;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.PublicKey;
import java.security.Security;
import static it.pagopa.pn.ciechecker.utils.ValidateUtils.*;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

@lombok.CustomLog
@RequiredArgsConstructor
@Data
@Service
public class CieCheckerImpl implements CieChecker, CieCheckerInterface {

    private static final Set<String> COMPATIBLE_ALGOS = Set.of(CieCheckerConstants.SHA_1, CieCheckerConstants.SHA_256, CieCheckerConstants.SHA_384, CieCheckerConstants.SHA_512);

    private final PnMandateConfig pnMandateConfig;

    private final S3BucketClient s3BucketClient;

    private List<X509Certificate> cscaAnchor;
    private String ciecheckerCscaAnchorPathFilename;

    @Override
    public void init() throws CieCheckerException {

        log.debug(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_INIT);
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        String cscaPath = pnMandateConfig.getCiecheckerCscaAnchorPathFilename();
        log.trace("CSCA ANCHOR PATH: {}", cscaPath );
        try {

            if(Objects.isNull(cscaPath) || cscaPath.isBlank()) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_INIT,ResultCieChecker.KO_EXC_NO_CSCA_ANCHORS_PROVIDED.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_CSCA_ANCHORS_PROVIDED);
            }
            InputStream inputStreamCscaAnchor;
            if (cscaPath.startsWith(PROTOCOLLO_S3) ){
               inputStreamCscaAnchor= s3BucketClient.getObjectContent(cscaPath);
            } else {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_INIT,ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS);
            }

            if (cscaPath.endsWith(".zip") || cscaPath.endsWith(".ZIP")) {
                  cscaAnchor = ValidateUtils.extractCscaAnchorFromZip(inputStreamCscaAnchor);
            } else if (cscaPath.endsWith(".pem") || cscaPath.endsWith(".PEM")) {
                  cscaAnchor = ValidateUtils.loadCertificateFromPemFile(inputStreamCscaAnchor);
            }

            this.setCscaAnchor(cscaAnchor);
        }catch (Exception e){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_INIT, e.getClass().getName()+ LogsConstant.MESSAGE +e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS, e);
        }
    }


    @Override
    public ResultCieChecker validateMandate(CieValidationData data) {
        log.logStartingProcess(LogsConstant.CIECHECKER_VALIDATE_MANDATE);

        CMSSignedData cms;
        try {
            ValidateUtils.validateDataInput(data);

            cms = new CMSSignedData(truncSodBytes(data.getCieIas().getSod()));
            data.getCieMrtd().setSod(truncSodBytes(data.getCieMrtd().getSod()));
            log.debug(LogsConstant.CIECHECKER_VALIDATE_MANDATE, "CMSSignedData={}", cms);
            //dopo aver controllato eventuali null facciamo pulizia dei dati (da possili padding) come step preliminare alle validazioni
            ValidateUtils.cleanAllCieFields(data);

            //16048-bis - NIS: nis_verify_sod.sh
            verifyDigitalSignature(cms);

            //16049 NIS: nis_verify_sod_passive_auth.sh
            verifySodPassiveAuthCie(cms, data.getCieIas().getNis());

            //16050 NIS: nis_verify_challenge.sh - verifica del nonce: verifica la firma di una challenge IAS
            verifyChallengeFromSignature(data);

            //16051 MRTD: verify_integrity.sh
            verifyIntegrity(data.getCieMrtd());

            //16052 MRTD: verify_signature.sh
            cms = new CMSSignedData(data.getCieMrtd().getSod());
            verifyDigitalSignature(cms);

            //16304 - Verifica codice fiscale del delegante con quanto presente nei dati della CIE
            verifyCodFiscDelegante(data);

            //16669 - verifica scadenza CIE
            ResultCieChecker result = verifyExpirationCie(data.getCieMrtd().getDg1());
            if(OK.equals(result.getValue()) )
                log.logEndingProcess(LogsConstant.CIECHECKER_VALIDATE_MANDATE, true, ResultCieChecker.OK.getValue(), null);
            else
                log.logEndingProcess(LogsConstant.CIECHECKER_VALIDATE_MANDATE, false, result.getValue(), null);
            return result;
        }catch(CMSException cmse){
            log.logEndingProcess(LogsConstant.CIECHECKER_VALIDATE_MANDATE, false, cmse.getClass().getName() + LogsConstant.MESSAGE  + ResultCieChecker.KO_EXC_GENERATE_CMSSIGNEDDATA.getValue(), cmse);
            return ResultCieChecker.KO_EXC_GENERATE_CMSSIGNEDDATA;
        }catch (CieCheckerException cce ) {
            log.logEndingProcess(LogsConstant.CIECHECKER_VALIDATE_MANDATE, false, cce.getClass().getName() + LogsConstant.MESSAGE  + cce.getResult().getValue(), cce);
            return cce.getResult();
        }catch (Exception e ) {
            log.logEndingProcess(LogsConstant.CIECHECKER_VALIDATE_MANDATE, false, e.getClass().getName() + LogsConstant.MESSAGE  + e.getMessage(), e);
            return ResultCieChecker.KO;
        }
    }


    private static byte[] truncSodBytes(byte [] inSod) {
    	return Arrays.copyOfRange(inSod, 4, inSod.length);
    }

    /**
     * Verifica codice fiscale del delegante con quanto presente nei dati della CIE
     * @param data CieValidationData
     * @return ResultCieChecker: OK se il codice fiscale matcha con quello presente nel DG11, KO se non matcha
     * @throws CieCheckerException result
     */
    public ResultCieChecker verifyCodFiscDelegante (CieValidationData data ) throws CieCheckerException{

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.VALIDATEUTILS_VERIFY_CODICEFISCALE_DELEGANTE);
        String codiceFiscaleDelegante = parserTLVTagValue(data.getCieMrtd().getDg11(), TAG_PERSONAL_NUMBER);
        if (data.getCodFiscDelegante().equals(codiceFiscaleDelegante))
            return ResultCieChecker.OK;
        else {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.VALIDATEUTILS_VERIFY_CODICEFISCALE_DELEGANTE, ResultCieChecker.KO_EXC_CODFISCALE_NOT_VERIFIED.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_CODFISCALE_NOT_VERIFIED);
        }
    }

    public ResultCieChecker verifyExpirationCie (byte[] dg1 ) throws CieCheckerException{

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_VERIFY_EXPIRATION_CIE);
        String dataElement = parserTLVTagValue(dg1, TAG_EXPIRE_DATE);

        String expirationDate = dataElement.substring(38, 38+6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");

        try {
            LocalDate inputDate = LocalDate.parse(expirationDate, formatter);
            LocalDate today = LocalDate.now();
            //    isAfter() restituisce true se l'oggetto chiamante è strettamente SUCCESSIVO
            //    all'oggetto passato come argomento.
            boolean isAfter = inputDate.isAfter(today);
            if(isAfter)
                return ResultCieChecker.OK;
            else
                return ResultCieChecker.KO_EXC_EXPIRATIONDATE;

        } catch (DateTimeParseException dtpe) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_EXPIRATION_CIE, DateTimeParseException.class + LogsConstant.MESSAGE  + dtpe.getMessage());
            throw new CieCheckerException( ResultCieChecker.KO_EXC_INVALID_EXPIRATIONDATE, dtpe );
        }
    }


    /**
     * @param data CieValidationData
     * @return ResultCieChecker
     * @throws CieCheckerException
     *  Verifica il challenge (nonce) dalla signature, confrontandolo con quello estratto dalla firma.
     *  Per la verifica si utilizzano : byte[] signature, byte[] pubKey, byte[] challenge
     */
    public ResultCieChecker verifyChallengeFromSignature(CieValidationData data) throws CieCheckerException {

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_VERIFY_CHALLENGE_FROM_SIGNATURE);
        try {
            RSAEngine engine = new RSAEngine();
            PKCS1Encoding engine2 = new PKCS1Encoding(engine);
            // estrazione public key dall'oggetto firma
            RSAKeyParameters publicKey = extractPublicKeyFromSignature(data.getCieIas().getPublicKey());
            engine2.init(false, publicKey);
            // estrae dalla signature i byte del nonce/challenge
            byte[] recovered = engine2.processBlock(data.getSignedNonce(), 0, data.getSignedNonce().length);
            if (!(Arrays.equals(recovered, data.getNonce().getBytes()))) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_CHALLENGE_FROM_SIGNATURE, ResultCieChecker.KO_EXC_NO_MATCH_NONCE_SIGNATURE.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_MATCH_NONCE_SIGNATURE);
            }else {
                log.info(LogsConstant.SUCCESSFUL_OPERATION_ON_LABEL, LogsConstant.CIECHECKER_VERIFY_CHALLENGE_FROM_SIGNATURE, LogsConstant.RESULTCHECKER, ResultCieChecker.OK.getValue());
                return ResultCieChecker.OK;
            }
        }catch (IllegalArgumentException ie){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_CHALLENGE_FROM_SIGNATURE, ie.getClass().getName() + LogsConstant.MESSAGE  +  ie.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_GENERATE_PUBLICKEY, ie);
        }catch( CryptoException cre){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_CHALLENGE_FROM_SIGNATURE, cre.getClass().getName() + LogsConstant.MESSAGE  + cre.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_INVALID_CRYPTOGRAPHIC_OPERATION, cre);
        }
    }


    /**
     * Converte la public key byte array in una public Key
     * @param pubKey byte[]
     * @return RSAKeyParameters result
     */
    private RSAKeyParameters extractPublicKeyFromSignature(byte[] pubKey) {
        RSAPublicKey pkcs1PublicKey;
        try {
            SubjectPublicKeyInfo spki = SubjectPublicKeyInfo.getInstance(pubKey);
            byte[] rawRsaKeyBytes = spki.getPublicKeyData().getBytes();
            pkcs1PublicKey = RSAPublicKey.getInstance(rawRsaKeyBytes);
        } catch (IllegalArgumentException iae) {
            pkcs1PublicKey = RSAPublicKey.getInstance(pubKey);
        }
        BigInteger modulus = pkcs1PublicKey.getModulus();
        BigInteger publicExponent = pkcs1PublicKey.getPublicExponent();

        return new RSAKeyParameters(false, modulus, publicExponent); // false per public key (true=private)
    }


    /**
     * Effettua la verifica completa del Document Security Object (SOD) della CIE
     * (nis_verify_sod_passive_auth.sh)
     * @param cms CMSSignedData
     * @param cieIasNis byte[]
     * @return boolean
     * @throws CieCheckerException ce
     */
    @Override
    public boolean verifySodPassiveAuthCie(CMSSignedData cms, byte[] cieIasNis) throws CieCheckerException {

        try {
            log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE);
            //*****************************************************
            // ** PASSO 1 - ANALISI E ESTRAZIONE DEI COMPONENTI
            // *****************************************************/
            X509CertificateHolder certHolder = ValidateUtils.extractDscCertDer(cms);

            PublicKey publicKey = ValidateUtils.extractPublicKeyFromHolder(certHolder);
            if ( Objects.isNull(publicKey) ) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, ResultCieChecker.KO_EXC_EXTRACTION_PUBLICKEY.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_EXTRACTION_PUBLICKEY);
            }

            List<byte[]> signatures = ValidateUtils.extractSignaturesFromSignedData(cms);
            if ( signatures.isEmpty() ) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, ResultCieChecker.KO_EXC_NO_SIGNATURES_SIGNED_DATA.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_SIGNATURES_SIGNED_DATA);
            }

            //*****************************************************
            // ** PASSO 2 - VERIFICA ed ESTRAZIONE DEGLI HASH: CONTENT
            // *****************************************************/
            if (!ValidateUtils.verifyMatchHashContent(cms)) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, ResultCieChecker.KO_EXC_NO_HASH_CONTENT_MATCH.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_HASH_CONTENT_MATCH);
            }

            //*******************************************************************
            // ** PASSO 1A: ESTRAZIONE DEGLI ATTRIBUTI FIRMATI (signedAttributes)
            // *******************************************************************/
            Hashtable<ASN1ObjectIdentifier, Attribute> signedAttributesTable = ValidateUtils.extractAllSignedAttributes(cms);
            if(Objects.isNull(signedAttributesTable) || signedAttributesTable.isEmpty()) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, ResultCieChecker.KO_EXC_NO_SIGNED_ATTRIBUTE.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_SIGNED_ATTRIBUTE);
            }

            //*******************************************************************
            // ** PASSO 1B: ANALISI DEGLI HASH DEI DATI (DataGroupHashes)
            // *********************************************************************/
            if (!ValidateUtils.verifyNisShaFromDataGroup(cms, cieIasNis)) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, ResultCieChecker.KO_EXC_NO_MATCH_NIS_HASHES_DATAGROUP.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_MATCH_NIS_HASHES_DATAGROUP);
            }

            ResultCieChecker result = ValidateUtils.verifySodPassiveDigitalSignature(cms, publicKey);
            if(!(result.getValue().equals(OK))) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, ResultCieChecker.KO_EXC_NOVALID_DIGITAL_SIGNATURE.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_DIGITAL_SIGNATURE);
            }else {
                log.info(LogsConstant.SUCCESSFUL_OPERATION_ON_LABEL, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, "boolean", true);
                return true;
            }
        }catch(CMSException cmse){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, cmse.getClass().getName() +LogsConstant.MESSAGE  + cmse.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_GENERATE_CMSSIGNEDDATA, cmse);
        } catch (Exception  e ){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE, e.getClass().getName() + LogsConstant.MESSAGE  + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

    /**
     * Accetta path EF.SOD e una lista variabile di path EF.DG* (varargs, così possiamo
     * passare un numero variabile di parametri senza dover creare un array a mano)
     * questo metodo replica il comportamento dello script verify_integrity.sh:
     * 1) decodifica il SOD (ValidateUtils.decodeSodHr)
     * 2) identifica l'algoritmo di hash
     * 3) verifica che sia compatibile
     * 4) per ogni DG passato ricalcola l'hash e lo confronta con quello nel SOD
     * In caso di eccezione ritorna false (puoi cambiare la gestione se preferisci lanciare eccezioni).
     */
    @Override
    public ResultCieChecker verifyIntegrity(CieMrtd mrtd) throws CieCheckerException {
        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_VERIFY_INTEGRITY);
        try {
            if(mrtd.getSod() == null){
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_INTEGRITY, ResultCieChecker.KO_EXC_NOTFOUND_MRTD_SOD.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NOTFOUND_MRTD_SOD);
            }
            byte[] dg1 = mrtd.getDg1() != null ? mrtd.getDg1() : null;
            byte[] dg11 = mrtd.getDg11() != null ? mrtd.getDg11() : null;

            return verifyIntegrityCore(mrtd.getSod(), dg1, dg11);
        } catch (CieCheckerException ce) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_INTEGRITY, ce.getClass().getName() + LogsConstant.MESSAGE  + ce.getMessage());
            throw new CieCheckerException(ce.getResult(), ce);
        } catch (Exception e) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_INTEGRITY, e.getClass().getName() + LogsConstant.MESSAGE  + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_DIGEST_NOT_VERIFIED, e);
        }
    }

    /**
     * Il metodo esegue la logica vera e propria e propaga le eccezioni.
     * - usa ValidateUtils.decodeSodHr(...) per ottenere SodSummary (equivalente a sod_summary dello script)
     * - per ogni DG contenuto in dgFiles verifica hash
     * Ritorna true solo se tutti i DG verificati combaciano.
     * @param sodBytes byte[]
     * @param dg1 byte[]
     * @param dg11 byte[]
     * @return result ResultCieChecker
     * @throws Exception e
     */
    public ResultCieChecker verifyIntegrityCore(byte[] sodBytes, byte[] dg1, byte[] dg11) throws Exception {
        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_VERIFY_INTEGRITY_CORE);
        SodSummary sodSummary = decodeSodHr(sodBytes);

        String hashAlgorithmName = getDigestName(sodSummary.getDgDigestAlgorithm().getAlgorithm().getId());
        if (hashAlgorithmName == null || hashAlgorithmName.isBlank()) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_INTEGRITY_CORE, ResultCieChecker.KO_EXC_NO_HASH_ALGORITHM_SOD.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_HASH_ALGORITHM_SOD);
        }
        if (!COMPATIBLE_ALGOS.contains(hashAlgorithmName)) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_INTEGRITY_CORE, ResultCieChecker.KO_EXC_UNSUPPORTED_ALGORITHM_SOD.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_UNSUPPORTED_ALGORITHM_SOD);
        }

        MessageDigest md = MessageDigest.getInstance(hashAlgorithmName);

        Map<Integer, byte[]> expectedHashes = sodSummary.getDgExpectedHashes();
        if (expectedHashes == null || expectedHashes.isEmpty()) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_INTEGRITY_CORE, ResultCieChecker.KO_EXC_NOTFOUND_EXPECTED_HASHES_SOD.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NOTFOUND_EXPECTED_HASHES_SOD);
        }

        verifyDigestList(md, dg1, expectedHashes, 1);
        verifyDigestList(md, dg11, expectedHashes, 11);

        log.info(LogsConstant.SUCCESSFUL_OPERATION_ON_LABEL, LogsConstant.CIECHECKER_VERIFY_INTEGRITY_CORE, "ResultCieChecker", ResultCieChecker.OK.getValue());
        return ResultCieChecker.OK;
    }

    private void verifyDigestList(MessageDigest md, byte[] dg, Map<Integer, byte[]> expectedHashes, int dgNum) throws CieCheckerException {
        if (!isVerifyDigest(md, dg, expectedHashes.get(dgNum))) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS_DG_VALIDATE, LogsConstant.CIECHECKER_VERIFY_DIGESTLIST, ResultCieChecker.KO_EXC_NOT_SAME_DIGEST.getValue(), dgNum);
            if(dgNum == 1) {
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NOT_SAME_DIGEST1);
            }else
                throw new CieCheckerException(ResultCieChecker.KO_EXC_NOT_SAME_DIGEST11);
        }
    }

    /**
     * Verifica la validità della catena di fiducia del SOD
     * @param cms CMSSignedData
     * @return ResultCieChecker
     */
    public ResultCieChecker verifyTrustChain(CMSSignedData cms) throws CieCheckerException {
        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_VERIFY_TRUST_CHAIN);
        try {
            X509CertificateHolder holder = ValidateUtils.extractDscCertDer(cms);
            byte[] dscDer = holder.getEncoded();

            ResultCieChecker result = ValidateUtils.verifyDscAgainstTrustBundle(dscDer, cscaAnchor, new Date());
            if (!result.getValue().equals(OK)) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_TRUST_CHAIN, ResultCieChecker.KO_EXC_CERTIFICATE_NOT_SIGNED.getValue());
                throw new CieCheckerException(ResultCieChecker.KO_EXC_CERTIFICATE_NOT_SIGNED);
            }
            log.info(LogsConstant.SUCCESSFUL_OPERATION_ON_LABEL, LogsConstant.CIECHECKER_VERIFY_TRUST_CHAIN, "ResultCieChecker", result.getValue());
            return ValidateUtils.verifyDigitalSignature(cms);
        }catch(IOException ioe){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_TRUST_CHAIN, ioe.getClass().getName() + LogsConstant.MESSAGE  + ioe.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_IOEXCEPTION, ioe);
        }
    }


    /**
     * Verifica la validità della firma e della catena di fiducia del SOD
     * nis_verify_sod.sh / verify_signature.sh
     * @param cms CMSSignedData
     * @return boolean
     */
    @Override
    public ResultCieChecker verifyDigitalSignature( CMSSignedData cms) throws CieCheckerException {
        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIECHECKER_VERIFY_DIGITAL_SIGNATURE);
        ResultCieChecker result = verifyTrustChain(cms);
        if( !(result.getValue().equals(OK)) ) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIECHECKER_VERIFY_DIGITAL_SIGNATURE, result.getValue());
            throw new CieCheckerException(result);
        } else {
            log.info(LogsConstant.SUCCESSFUL_OPERATION_ON_LABEL, LogsConstant.CIECHECKER_VERIFY_DIGITAL_SIGNATURE, result.getValue());
            return result;
        }
    }


    public InputStream getContentCscaAnchorFile(String key) {
        return s3BucketClient.getObjectContent(key);
    }


}
