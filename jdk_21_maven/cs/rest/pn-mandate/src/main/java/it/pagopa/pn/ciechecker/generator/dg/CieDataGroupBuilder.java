package it.pagopa.pn.ciechecker.generator.dg;

import com.payneteasy.tlv.BerTag;
import com.payneteasy.tlv.BerTlvBuilder;
import it.pagopa.pn.ciechecker.generator.model.MrzData;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
public class CieDataGroupBuilder {

    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    public byte[] buildDG1(String surname,
                           String givenName,
                           String documentNumber,
                           String nationality,
                           LocalDate birthDate,
                           char sex,
                           LocalDate expiryDate) {

        MrzData mrzData = new MrzData(
                "I<",
                nationality,            // (issuing state)
                documentNumber,
                birthDate,
                sex,
                expiryDate,
                nationality,
                surname,
                givenName,
                "",
                ""
        );
        String mrz = buildMrz(mrzData);

        BerTag tDG1 = new BerTag(0x61);
        BerTag tMRZ = new BerTag(0x5F, 0x1F);

        BerTlvBuilder inner = new BerTlvBuilder();
        inner.addBytes(tMRZ, mrz.getBytes(StandardCharsets.US_ASCII));

        BerTlvBuilder outer = new BerTlvBuilder();
        outer.addBytes(tDG1, inner.buildArray());

        return outer.buildArray();
    }

    public String buildMrz(final MrzData data) {

        final String cleanDocCode = nz(data.getDocCode()).toUpperCase().replace(' ', '<');
        final String cleanIssuingState = nz(data.getIssuingState()).toUpperCase();
        final String cleanNationality = nz(data.getNationality()).toUpperCase();
        final String cleanDocNumber = nz(data.getDocumentNumber()).toUpperCase().replace(' ', '<');
        final String cleanSurname = nz(data.getSurname()).toUpperCase().replace(' ', '<');
        final String cleanGivenNames = nz(data.getGivenNames()).toUpperCase().replace(' ', '<');

        final String dobYYMMDD = data.getBirthDate().format(YYMMDD);
        final String expYYMMDD = data.getExpiryDate().format(YYMMDD);
        final char sexMrz = (data.getSex() == 'M' || data.getSex() == 'F') ? data.getSex() : '<';

        //LINEA 1
        final String blockDocCode2 = padRight(cleanDocCode, 2, '<').substring(0, 2);
        final String blockIssuingState3 = padRight(cleanIssuingState, 3, '<').substring(0, 3);
        final String blockNationality3 = padRight(cleanNationality, 3, '<').substring(0, 3);
        final String blockDocNumber9 = padRight(cleanDocNumber, 9, '<').substring(0, 9);

        final String blockOptional1_15 = padRight(sanitizeOpt(data.getOptional1()), 15, '<');
        final String blockOptional2_11 = padRight(sanitizeOpt(data.getOptional2()), 11, '<');

        final char cdDoc = (char) ('0' + checkDigit(blockDocNumber9));

        String line1 = blockDocCode2 + blockIssuingState3 + blockDocNumber9 + cdDoc + blockOptional1_15;
        line1 = padRight(line1, 30, '<').substring(0, 30);

        //LINEA 2
        final char cdDob = (char) ('0' + checkDigit(dobYYMMDD));
        final char cdExp = (char) ('0' + checkDigit(expYYMMDD));

        final String compositeInput = blockDocNumber9 + cdDoc + dobYYMMDD + cdDob + expYYMMDD + cdExp + blockOptional2_11;
        final char cdComposite = (char) ('0' + checkDigit(compositeInput));

        final String line2Body = dobYYMMDD + cdDob + sexMrz + expYYMMDD + cdExp + blockNationality3 + blockOptional2_11;

        String line2 = line2Body.substring(0, 29) + cdComposite;
        line2 = padRight(line2, 30, '<').substring(0, 30);

        //LINEA 3
        final String nameField = cleanSurname + "<<" + cleanGivenNames;
        final String line3 = padRight(nameField, 30, '<').substring(0, 30);

        return line1 + line2 + line3;
    }


    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String sanitizeOpt(String s) {
        return (s == null || s.isEmpty()) ? "" : s.toUpperCase().replace(' ', '<');
    }

    public byte[] buildDG11(String fullName, String cf, LocalDate birthDate, String placeOfBirth, String nationality)
            throws IOException {
        Instant instant = birthDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        ASN1EncodableVector seq = new ASN1EncodableVector();
        seq.add(new DERPrintableString(fullName));
        seq.add(new DERPrintableString(cf));
        seq.add(new DERGeneralizedTime(Date.from(instant)));
        seq.add(new DERPrintableString(placeOfBirth));
        seq.add(new DERPrintableString(nationality));
        DERSequence dg11 = new DERSequence(seq);
        return dg11.getEncoded(ASN1Encoding.DER);
    }

    public byte[] buildDG11(String fullName,
                            String codiceFiscale,
                            LocalDate birthDate,
                            String placeOfBirth,
                            String address,
                            String telephone,
                            String profession) {

        BerTag tDG11 = new BerTag(0x6B);
        BerTag tTAGLIST = new BerTag(0x5C);
        BerTag tFULLNAME = new BerTag(0x5F, 0x0E);
        BerTag tPERSONALNUMBER = new BerTag(0x5F, 0x10);
        BerTag tDOB = new BerTag(0x5F, 0x2B);
        BerTag tPOB = new BerTag(0x5F, 0x11);
        BerTag tADDR = new BerTag(0x5F, 0x42);
        BerTag tTEL = new BerTag(0x5F, 0x12);
        BerTag tPROF = new BerTag(0x5F, 0x13);

        List<byte[]> presentTags = new ArrayList<>();
        BerTlvBuilder inner = new BerTlvBuilder();

        if (fullName != null) {
            inner.addBytes(tFULLNAME, fullName.getBytes(StandardCharsets.UTF_8));
            presentTags.add(new byte[]{(byte) 0x5F, (byte) 0x0E});
        }
        if (codiceFiscale != null) {
            inner.addBytes(tPERSONALNUMBER, codiceFiscale.getBytes(StandardCharsets.UTF_8));
            presentTags.add(new byte[]{(byte) 0x5F, (byte) 0x10});
        }
        if (birthDate != null) {
            String yyyymmdd = birthDate.format(DateTimeFormatter.BASIC_ISO_DATE);
            inner.addBytes(tDOB, yyyymmdd.getBytes(StandardCharsets.US_ASCII));
            presentTags.add(new byte[]{(byte) 0x5F, (byte) 0x2B});
        }
        if (placeOfBirth != null) {
            inner.addBytes(tPOB, placeOfBirth.getBytes(StandardCharsets.UTF_8));
            presentTags.add(new byte[]{(byte) 0x5F, (byte) 0x11});
        }
        if (address != null) {
            inner.addBytes(tADDR, address.getBytes(StandardCharsets.UTF_8));
            presentTags.add(new byte[]{(byte) 0x5F, (byte) 0x42});
        }
        if (telephone != null) {
            inner.addBytes(tTEL, telephone.getBytes(StandardCharsets.UTF_8));
            presentTags.add(new byte[]{(byte) 0x5F, (byte) 0x12});
        }
        if (profession != null) {
            inner.addBytes(tPROF, profession.getBytes(StandardCharsets.UTF_8));
            presentTags.add(new byte[]{(byte) 0x5F, (byte) 0x13});
        }

        int total = presentTags.stream().mapToInt(t -> t.length).sum();
        byte[] tagListValue = new byte[total];
        int off = 0;
        for (byte[] t : presentTags) {
            System.arraycopy(t, 0, tagListValue, off, t.length);
            off += t.length;
        }

        BerTlvBuilder innerWithList = new BerTlvBuilder();
        innerWithList.addBytes(tTAGLIST, tagListValue);

        BerTlvBuilder outer = new BerTlvBuilder();
        outer.addBytes(tDG11, concat(innerWithList.buildArray(), inner.buildArray()));

        return outer.buildArray();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }


    private String padRight(String input, int length, char pad) {
        if (input.length() >= length) return input.substring(0, length);
        char[] padArray = new char[length - input.length()];
        Arrays.fill(padArray, pad);
        return input + new String(padArray);
    }

    private int checkDigit(String data) {
        int[] weights = {7, 3, 1};
        int sum = 0;
        int i = 0;
        for (char c : data.toCharArray()) {
            int val;
            if (Character.isDigit(c)) val = c - '0';
            else if (c >= 'A' && c <= 'Z') val = c - 'A' + 10;
            else val = 0;
            sum += val * weights[i % 3];
            i++;
        }
        return sum % 10;
    }

}
