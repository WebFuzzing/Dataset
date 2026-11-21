package it.pagopa.pn.ciechecker.generator.dg;

import com.payneteasy.tlv.BerTag;
import com.payneteasy.tlv.BerTlv;
import com.payneteasy.tlv.BerTlvParser;
import com.payneteasy.tlv.BerTlvs;
import it.pagopa.pn.ciechecker.generator.model.MrzData;
import org.bouncycastle.asn1.ASN1Sequence;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CieDataGroupBuilderTest {

    private static final String NAME = "NAME";
    private static final String SURNAME = "SURNAME";
    private static final String DOC_NUMBER = "DOCNUMBER";
    private static final String NATIONALITY = "ITA";
    private static final String CF= "RSSMRA80A01H501U";
    private static final char SEX = 'M';
    private static final String POB = "PLACEOFBIRTH";
    private static final int LENGTH =90;
    private static final LocalDate DOB = LocalDate.of(1990, 1, 1);
    private static final LocalDate EXP = LocalDate.of(2030, 1, 1);


    @Test
    public void buildDG1_validMrzFormat() {
        byte[] dg1 = buildMrz();
        String mrz = new String(dg1);
        assertEquals(LENGTH, mrz.length());
        assertTrue(mrz.startsWith("I<"+NATIONALITY));
        assertTrue(mrz.contains(SURNAME));
    }
    @Test
    public void buildDG1_tlvStructure_andMrzContent_isCorrect() {
        CieDataGroupBuilder b = new CieDataGroupBuilder();

        byte[] dg1 = b.buildDG1(
                SURNAME, NAME, DOC_NUMBER, NATIONALITY, DOB, SEX, EXP
        );

        BerTlvParser parser = new BerTlvParser();
        BerTag tDG1 = new BerTag(0x61);        // constructed
        BerTag tMRZ = new BerTag(0x5F, 0x1F);  // primitive

        // parse una sola volta l'intera struttura
        BerTlvs tlvs = parser.parse(dg1, 0, dg1.length);

        // 61 deve esistere ed essere constructed
        BerTlv dg1Tlv = tlvs.find(tDG1);
        assertNotNull(dg1Tlv, "DG1 outer TLV (tag 0x61) must exist");
        assertTrue(dg1Tlv.isConstructed(), "DG1 (61) must be constructed");

        // cerca direttamente 5F1F in profondità (non estrarre bytes dal 61)
        BerTlv mrzTlv = tlvs.find(tMRZ);
        assertNotNull(mrzTlv, "MRZ TLV (tag 5F1F) must exist");
        assertFalse(mrzTlv.isConstructed(), "MRZ (5F1F) must be primitive");

        byte[] mrzBytes = mrzTlv.getBytesValue();               // OK: primitivo
        String mrz = new String(mrzBytes, StandardCharsets.US_ASCII);
        assertEquals(90, mrz.length(), "TD1 MRZ must be 90 chars");

        // split in 3 linee e fai i check come prima...
        String l1 = mrz.substring(0, 30);
        String l2 = mrz.substring(30, 60);
        String l3 = mrz.substring(60, 90);

        assertTrue(l1.startsWith("I<" + NATIONALITY));
        assertTrue(l3.contains(SURNAME));
        assertTrue(l3.contains(NAME));
        assertTrue(mrz.matches("[A-Z0-9<]{90}"), "MRZ must contain only A–Z, 0–9, <");

        // Verifiche posizionali + check digit come avevi già impostato:
        String docNum9 = l1.substring(5, 14);
        char cdDoc = l1.charAt(14);

        String dobYYMMDD = l2.substring(0, 6);
        char cdDob = l2.charAt(6);
        char sexMrz = l2.charAt(7);
        String expYYMMDD = l2.substring(8, 14);
        char cdExp = l2.charAt(14);
        String nat3 = l2.substring(15, 18);
        String opt2 = l2.substring(18, 29); // 11 chars
        char cdComposite = l2.charAt(29);

        assertEquals(NATIONALITY, nat3);
        assertEquals(SEX == 'M' || SEX == 'F' ? SEX : '<', sexMrz);
        assertTrue(dobYYMMDD.matches("\\d{6}"));
        assertTrue(expYYMMDD.matches("\\d{6}"));

        assertEquals(cdDoc, calcCd(docNum9));
        assertEquals(cdDob, calcCd(dobYYMMDD));
        assertEquals(cdExp, calcCd(expYYMMDD));

        String compositeInput = docNum9 + cdDoc + dobYYMMDD + cdDob + expYYMMDD + cdExp + opt2;
        assertEquals(cdComposite, calcCd(compositeInput));
    }


    @Test
    public void buildDG11_asn1Parsable() throws Exception {
        byte[] dg11 = buildDG11();
        ASN1Sequence seq = ASN1Sequence.getInstance(new ByteArrayInputStream(dg11).readAllBytes());
        assertEquals(5, seq.size());
        assertTrue(seq.toString().contains(POB));
        assertTrue(seq.toString().contains(NAME));
        assertTrue(seq.toString().contains(CF));
    }


    private byte[] buildMrz(){

        MrzData mrzData = new MrzData(
                "I<",
                NATIONALITY,
                DOC_NUMBER,
                DOB,
                SEX,
                EXP,
                NATIONALITY,
                SURNAME,
                NAME,
                null,
                null);
        return new CieDataGroupBuilder().buildMrz(mrzData).getBytes();

    }

    private byte[] buildDG11() throws IOException {
        CieDataGroupBuilder b = new CieDataGroupBuilder();
        String nameSurname= NAME + " " + SURNAME;
        return b.buildDG11(
                nameSurname,
                CF,
                DOB,
                POB,
                NATIONALITY
        );
    }

    private static char calcCd(String data) {
        int[] w = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < data.length(); i++) {
            sum += mrzVal(data.charAt(i)) * w[i % 3];
        }
        return (char) ('0' + (sum % 10));
    }

    private static int mrzVal(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'Z') return c - 'A' + 10;
        if (c == '<') return 0;
        return 0;
    }
}
