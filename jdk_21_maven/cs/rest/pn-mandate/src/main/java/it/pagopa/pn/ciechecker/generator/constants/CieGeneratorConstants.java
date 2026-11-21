package it.pagopa.pn.ciechecker.generator.constants;

import java.time.LocalDate;

public class CieGeneratorConstants {

    private CieGeneratorConstants(){}

    public static final String MASTERLIST_FILENAME ="new_IT_MasterListCSCA.zip";
    public static final String CA_CERT_FILENAME ="catest.pem";
    public static final String CA_KEY_FILENAME ="catest.key";

    public static final String CTX_VALIDATION_DATA ="validationData";
    public static final String CTX_DOCUMENT_SIGNER_INFO ="cieCaAndKey";


    //PARAMETERS
    public static final String DEFAULT_GIVEN_NAME = "MARIO";
    public static final String DEFAULT_SURNAME = "ROSSI";
    public static final String DEFAULT_DOCUMENT_NUMBER = "AA1234567";
    public static final String DEFAULT_NATIONALITY = "ITA";
    public static final String DEFAULT_CODICE_FISCALE ="RSSMRA80A01H501U";
    public static final String DEFAULT_PLACE_OF_BIRTH = "ROMA";
    public static final char DEFAULT_SEX = 'M';
    public static final LocalDate DEFAULT_DATE_OF_BIRTH = LocalDate.of(1980,1,1);
    public static final LocalDate DEFAULT_EXPIRY_DATE =  LocalDate.of(2030, 12, 31);

}
