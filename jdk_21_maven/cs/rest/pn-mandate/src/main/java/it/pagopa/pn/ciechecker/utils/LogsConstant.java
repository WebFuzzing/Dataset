package it.pagopa.pn.ciechecker.utils;


public abstract class LogsConstant {

    // METODO API
    public static final String CIECHECKER_VALIDATE_MANDATE = "CieCheckerImpl.validateMandate()";

    public static final String CIECHECKER_INIT = "CieCheckerImpl.init()";

    // METODI
    public static final String CIECHECKER_VALIDATE_DATA_INPUT = "ValidateUtils.validateDataInput()";
    public static final String CIECHECKER_VERIFY_DIGITAL_SIGNATURE = "CieCheckerImpl.verifyDigitalSignature()";
    public static final String CIECHECKER_VERIFY_TRUST_CHAIN = "CieCheckerImpl.verifyTrustChain()";
    public static final String CIECHECKER_VERIFY_SOD_PASSIVE_AUTH_CIE = "CieCheckerImpl.verifySodPassiveAuthCie()";
    public static final String CIECHECKER_VERIFY_CHALLENGE_FROM_SIGNATURE = "CieCheckerImpl.verifyChallengeFromSignature()";
    public static final String CIECHECKER_VERIFY_INTEGRITY = "CieCheckerImpl.verifyIntegrity()";
    public static final String CIECHECKER_VERIFY_INTEGRITY_CORE = "CieCheckerImpl.verifyIntegrityCore()";
    public static final String VALIDATEUTILS_DECEODESODHR = "ValidateUtils.decodeSodHr()";
    public static final String CIECHECKER_VERIFY_DIGESTLIST = "CieCheckerImpl.verifyDigestList()";
    public static final String VALIDATEUTILS_VERIFY_DSC_AGAINST_TRUST_BUNDLE = "ValidateUtils.verifyDscAgainstTrustBundle()";
    public static final String VALIDATEUTILS_EXTRACT_DSC_CERT_DER = "ValidateUtils.extractDscCertDer()";
    public static final String VALIDATEUTILS_EXTRACT_PUBLICKEY_FROM_HOLDER = "ValidateUtils.extractPublicKeyFromHolder()";
    public static final String VALIDATEUTILS_VERIFY_MATCH_HASHCONTENT = "ValidateUtils.verifyMatchHashContent()";
    public static final String VALIDATEUTILS_VERIFY_OCTECTSTRINGS = "ValidateUtils.verifyOctetStrings()";
    public static final String VALIDATEUTILS_CALCULATE_DIGEST = "ValidateUtils.calculateDigest()";
    public static final String VALIDATEUTILS_EXTRACT_HASHSIGNED = "ValidateUtils.extractHashSigned()";
    public static final String VALIDATEUTILS_EXTRACT_ALLSIGNEDATTR = "ValidateUtils.extractAllSignedAttributes()";
    public static final String VALIDATEUTILS_EXTRACT_DATAGROUP = "ValidateUtils.extractDataGroupHashes()";
    public static final String VALIDATEUTILS_EXTRACT_CSCAANCHOR_ZIP = "ValidateUtils.extractCscaAnchorFromZip()";
    public static final String VALIDATEUTILS_GETX509CERTLIST_ZIPFILE = "ValidateUtils.getX509CertListFromZipFile()";
    public static final String VALIDATEUTILS_VERIFY_DIGITAL_SIGNATURE = "ValidateUtils.verifyDigitalSignature()";
    public static final String VALIDATEUTILS_VERIFY_SOD_PASS_DIGITAL_SIGNATURE = "ValidateUtils.verifySodPassiveDigitalSignature()";
    public static final String VALIDATEUTILS_VERIFY_CODICEFISCALE_DELEGANTE = "ValidateUtils.verifyCodFiscDelegante()";
    public static final String VALIDATEUTILS_EXTRACT_CODICEFISCALE_DELEGANTE = "ValidateUtils.extractCodiceFiscaleByOid()";
    public static final String S3BUCKETCLIENTIMPL_GET_OBJECT_CONTENT = "S3BucketClientImpl.getObjectContent()";
    public static final String S3BUCKETCLIENTIMPL_PUT_OBJECT_CONTENT = "S3BucketClientImpl.uploadContent()";
    public static final String VALIDATEUTILS_LOAD_CSCAANCHOR_PEM = "ValidateUtils.loadCertificateFromPemFile()";
    public static final String VALIDATEUTILS_PARSER_TLV_TAGVALUE = "ValidateUtils.parserTLVTagValue()";
    public static final String CIECHECKER_VERIFY_EXPIRATION_CIE = "CieCheckerImpl.verifyExpirationCie()";
    public static final String MASTERLISTMERGETOOL_ADDFILETOMASTERZIP = "MasterListMergeToolUtility.addFileToMasterListZip()";
    public static final String MASTERLISTMERGETOOL_UPLOADMASTERZIPTOS3 = "MasterListMergeToolUtility.writeNewMasterZip()";
    public static final String MASTERLISTMERGETOOL_MERGE = "MasterListMergeToolUtility.merge()";
    public static final String CIEFILEGENERATOR_VALIDATE_DATA_INPUT = "ValidateUtils.validateCieDataInput()";
    public static final String CIEFILEGENERATOR_EXPORTFILES = "CieFileGenerator.exportCieArtifactsToFiles()";
    public static final String VALIDATEUTILS_CLEANING_PADDING = "ValidateUtils.cleanAllCieFields()";
    public static final String CIECLEANUTILS_CLEANING_PADDING_IAS = "CieCleanUtils.cleanIas()";
    public static final String CIECLEANUTILS_CLEANING_PADDING_MRTD = "CieCleanUtils.cleanMrtd()";

    // ERROR
    public static final String ENDING_PROCESS_WITH_ERROR = "Ending '{}' Process with error = '{}' - '{}'";

    public static final String SUCCESSFUL_OPERATION_ON_LABEL = "Successful operation on '{}' : '{}' = {}";
    public static final String SUCCESSFUL_OPERATION_NO_RESULT_LABEL = "Successful operation: '{}'";
    public static final String EXCEPTION_IN_PROCESS = "Exception in '{}' - ResultCieChecker: {}";
    public static final String EXCEPTION_IN_PROCESS_DG_VALIDATE = "Exception in '{}' - ResultCieChecker: {} for DG number: {}";

    public static final String INVOKING_OPERATION_LABEL_WITH_ARGS = "Invoking operation '{}' with args: {}";
    public static final String INVOKING_OPERATION_LABEL = "Invoking operation '{}'";
    public static final String MESSAGE = " - Message: ";
    public static final String RESULTCHECKER = "ResultCieChecker: ";
    public static final String RESULTCHECKER_WITH_ARG = "ResultCieChecker: {}";

    private LogsConstant() {
        throw new IllegalStateException("LogsCostant is a utility class");
    }
}
