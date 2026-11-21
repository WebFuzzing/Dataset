package it.pagopa.pn.ciechecker.utils;

public class CieCheckerConstants {

    private CieCheckerConstants() {}

    // constants
    public static final String RSA_ALGORITHM = "RSA";
    public static final String X_509 = "X.509";
    public static final String PKIX = "PKIX";
    public static final String BOUNCY_CASTLE_PROVIDER = "BC";
    public static final String SHA_1 = "SHA-1";
    public static final String SHA_256 = "SHA-256";
    public static final String SHA_384 = "SHA-384";
    public static final String SHA_512 = "SHA-512";
    public static final String SHA_1_WITH_RSA = "SHA1withRSA";
    public static final String SHA_256_WITH_RSA = "SHA256withRSA";
    public static final String TAG_PERSONAL_NUMBER = "5F10";
    public static final String TAG_EXPIRE_DATE = "5F1F";
    public static final String PROTOCOLLO_S3 = "s3://";
    public static final String CSCA_ANCHOR_PATH_FILENAME = "src/test/resources/IT_MasterListCSCA.zip";
    public static final String RSA_ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding";

    public static final String CERTIFICATE_START= "-----BEGIN CERTIFICATE-----\n";
    public static final String CERTIFICATE_END = "\n-----END CERTIFICATE-----\n";

    public static final String OK = "OK";

    // exception messages
    public static final String EXC_KO = "Error";
    public static final String EXC_NOTFOUND_CERTIFICATES ="No certificates found in PKCS7";
    public static final String EXC_NOTFOUND_CMSSIGNEDDATA = "Not found CMSSignedData";
    public static final String EXC_PARSED_ZERO_CSCA_CERTIFICATES="Parsed 0 CSCA certificates";
    public static final String EXC_NO_CSCA_ANCHORS_PROVIDED= "No CSCA Anchor Files provided or is not valid";
    public static final String EXC_NOVALID_URI_CSCA_ANCHORS= "URI S3 for CSCA Anchor Files is not valid";
    public static final String EXC_NOVALID_CSCA_ANCHORS= "CSCA Anchor File is not valid (Valid file is .zip or .pem)";
    public static final String EXC_INPUT_PARAMETER_NULL="Input object parameter CieValidationData is null";
    public static final String EXC_NO_SIGNATURES_SIGNED_DATA="No signatures found in SignedData";
    public static final String EXC_NO_HASH_CONTENT_MATCH="No HashContent match";
    public static final String EXC_NO_HASH_SIGNED_DATA = "No hash of SignedData found";
    public static final String EXC_NO_SIGNED_ATTRIBUTE = "No SignedAttributes found";
    public static final String EXC_NO_MATCH_NIS_HASHES_DATAGROUP = "The NIS hashes DataGroup do not match with the expected value";
    public static final String EXC_CERTIFICATE_NOT_SIGNED = "The certificate has not been signed";
    public static final String EXC_NO_SIGNERINFORMATION = "SignerInformation is null";
    public static final String EXC_PARSING_CERTIFICATION = "Error during the conversion of the certificate to obtain the public key";
    public static final String EXC_GENERATE_CERTIFICATE = "Error in generating the certificate object and initializes";
    public static final String EXC_VALIDATE_CERTIFICATE = "Path does not chain with any of the trust anchors";
    public static final String EXC_GENERATE_CMSSIGNEDDATA = "Error in generating CMSSignedData";
    public static final String EXC_NO_SUPPORTED_CERTIFICATEFACTORY = "No Provider supports a CertificateFactorySpi implementation for the specified type";
    public static final String EXC_NO_SUPPORTED_CERTIFICATEPATHVALIDATOR = "No Provider supports a CertPathValidatorSpi implementation for the specified algorithm";
    public static final String EXC_INVALID_PARAMETER_CERTPATHVALIDATOR = "The specified parameters or the type of the specified CertPath are inappropriate for this CertPathValidator";
    public static final String EXC_IOEXCEPTION = "KO_IOEXCEPTION";
    public static final String EXC_NO_HASH_ALGORITHM_SOD = "Unable to determine hash algorithm from SOD";
    public static final String EXC_UNSUPPORTED_HASH_ALGORITHM_SOD = "Unsupported hash algorithm for SOD";
    public static final String EXC_NO_EXPECTED_HASHES_FOUND_SOD = "No expected hashes found in SOD";
    public static final String EXC_NOTFOUND_DIGEST = "One ore more digest not found.";
    public static final String EXC_NOTFOUND_MRTD_SOD = "Mrtd SOD is empty: must be present";
    public static final String EXC_GENERATE_PUBLICKEY = "Error in generating PublicKey Object";
    public static final String EXC_EXTRACTION_PUBLICKEY = "Error in extraction PublicKey Object";
    public static final String EXC_NO_MATCH_NONCE_SIGNATURE = "The challenge (nonce) from the signature does not match the one extracted from the signature.";
    public static final String EXC_PARSING_HEX_BYTE = "Conversion error from string to byte[]";
    public static final String EXC_INVALID_CRYPTOGRAPHIC_OPERATION = "Unexpected or invalid state occurs during cryptographic operations";
    public static final String EXC_NOFOUND_SIGNER ="No signer found";
    public static final String EXC_NO_CMSTYPEDDATA = "Invalid or unavailable signed content";
    public static final String EXC_NO_SIGNERINFORMATIONSTORE ="SignerInformationStore is null";
    public static final String EXC_NO_MESSAGEDIGESTSPI_SUPPORTED = "No Provider supports a MessageDigestSpi implementation for the specified algorithm";
    public static final String EXC_INVALID_CMSTYPEDDATA = "The signed content is not a valid hash sequence.";
    public static final String EXC_NO_NIS_HASHES_DATAGROUP ="The NIS hashes DataGroup List is null";
    public static final String EXC_ERROR_CREATE_VERIFIER = "Unable to create verifier";
    public static final String EXC_NOVALID_DIGITAL_SIGNATURE = "No valid digital Signature";
    public static final String KO_EXC_ERROR_SOD_DECODE= "Error in decode Sod Mrtd";
    public static final String EXC_NOT_SAME_DIGEST = "Digest mismatch between expected and actual DG";
    public static final String EXC_NOT_SAME_DIGEST_1 = "Digest mismatch between expected and actual DG1";
    public static final String EXC_NOT_SAME_DIGEST_11 = "Digest mismatch between expected and actual DG11";
    public static final String EXC_DIGEST_NOT_VERIFIED = "Digest are different, validation of integrity not passed";
    public static final String EXC_INVALID_PARAMETER_CIESOD = "Input parameter SOD CIE is null";
    public static final String EXC_INVALID_PARAMETER_MRTDSOD = "Input parameter SOD MRTD is null";
    public static final String EXC_INVALID_PARAMETER_CIENIS = "Input parameter NIS CIE is null";
    public static final String EXC_INVALID_PARAMETER_PUBLICKEY = "Input parameter PUBLICKEY CIE is null";
    public static final String EXC_INVALID_PARAMETER_SIGNEDNONCE = "Input parameter SIGNED NONCE CIE is null";
    public static final String EXC_INVALID_PARAMETER_NONCE = "Input parameter NONCE CIE is null";
    public static final String EXC_INVALID_PARAMETER_MRTDDG1 = "Input parameter DG1 MRTD is null";
    public static final String EXC_INVALID_PARAMETER_MRTDDG11 = "Input parameter DG11 MRTD is null";
    public static final String EXC_INVALID_PARAMETER_CODFISCDELEGANTE = "Input parameter CODICE FISCALE DELEGANTE is null";
    public static final String EXC_INVALID_PARAMETER_CIECAANDKEY = "Input parameter CIECAANDKEY is null";
    public static final String EXC_INVALID_VERIFIER = "Signed Attributes are not valid or do not match the data";
    public static final String EXC_INVALID_SIGNATURE = "Signature object is not initialized properly";
    public static final String EXC_INVALID_ALGORITHM = "No Provider supports a Signature implementation for the specified algorithm";
    public static final String EXC_INVALID_PUBLICKEY = "PublicKey is not valid";
    public static final String EXC_DECODER_ERROR = "Error occurs during the decoding process of data";
    public static final String EXC_NOFOUND_CODFISCALE_DG11 = "Error occurs during the extraction of Personal number in DG11";
    public static final String EXC_CODFISCALE_NOT_VERIFIED = "Personal number do not match with the expected value";
    public static final String EXC_NOVALID_CONNECT_S3 = "Problem with connect S3";
    public static final String EXC_NOFOUND_EXPIRE_DATE_DG1 = "Error occurs during the extraction of Expire Date in DG1";
    public static final String EXC_NOFOUND_TAG_DG = "Error occurs during the extraction of TAG in DG";
    public static final String EXC_INVALID_EXPIRATIONDATE = "The date string (Date of Expiry) is invalid or is not in the 'ddmmyy' format";
    public static final String EXC_EXPIRATIONDATE = "The CIE has expired";
    public static final String EXC_ISSUER_NULL = "issuer must not be null";
    public static final String EXC_DOC_SIGNER_CERT_NULL = "Document Signer must not be null";
    public static final String EXC_DOC_SIGNER_KEY_NULL = "Document Signer private key must not be null";
    public static final String EXC_IAS_SIGN_FAILED = "IAS private sign failed";
    public static final String EXC_DER_ENCODING_FAILED = "DER encoding failed";
    public static final String EXC_PEM_ENCODING_FAILED = "PEM encoding failed";
    public static final String EXC_CERT_ISSUANCE_FAILED = "Certificate issuance failed";
    public static final String EXC_NOFOUND_FILEARGS = "MasterList ZIP file or Certificate PEM file not found";
    public static final String EXC_CREATION_FILEZIPTEMP = "Error creating the new MasterList ZIP archive";
    public static final String EXC_UPLOAD_NEWFILEZIP_TO_S3 = "Error uploading the new MasterList ZIP archive on S3Bucket";

    public static final String EXC_INVALID_PARAMETER_CERTPEM = "Input parameter CERT PEM is null";
    public static final String EXC_INVALID_PARAMETER_CERTKEY = "Input parameter CERT KEY is null";
    public static final String EXC_INVALID_PARAMETER_BASEDIR = "Input parameter BASEDIR is null";
    public static final String EXC_DELETEEXISTFILE = "Errore durante l'eliminazione del file esistente";
    public static final String EXC_ATTRIBUTO_NULL = "Attributo is null";

    public static final String EXC_CLEANING_PADDING = "Impossible to clean, attribute is not valid for cleaning padding";
}
