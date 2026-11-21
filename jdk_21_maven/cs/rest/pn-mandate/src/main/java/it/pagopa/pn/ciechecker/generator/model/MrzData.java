package it.pagopa.pn.ciechecker.generator.model;

import java.time.LocalDate;

public final class MrzData {

    private final String docCode;
    private final String issuingState;
    private final String documentNumber;
    private final LocalDate birthDate;
    private final char sex;
    private final LocalDate expiryDate;
    private final String nationality;
    private final String surname;
    private final String givenNames;
    private final String optional1;
    private final String optional2;

    public MrzData(String docCode, String issuingState, String documentNumber, LocalDate birthDate,
                   char sex, LocalDate expiryDate, String nationality, String surname,
                   String givenNames, String optional1, String optional2) {
        this.docCode = docCode;
        this.issuingState = issuingState;
        this.documentNumber = documentNumber;
        this.birthDate = birthDate;
        this.sex = sex;
        this.expiryDate = expiryDate;
        this.nationality = nationality;
        this.surname = surname;
        this.givenNames = givenNames;
        this.optional1 = optional1;
        this.optional2 = optional2;
    }

    public String getDocCode() { return docCode; }
    public String getIssuingState() { return issuingState; }
    public String getDocumentNumber() { return documentNumber; }
    public LocalDate getBirthDate() { return birthDate; }
    public char getSex() { return sex; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getNationality() { return nationality; }
    public String getSurname() { return surname; }
    public String getGivenNames() { return givenNames; }
    public String getOptional1() { return optional1; }
    public String getOptional2() { return optional2; }

}