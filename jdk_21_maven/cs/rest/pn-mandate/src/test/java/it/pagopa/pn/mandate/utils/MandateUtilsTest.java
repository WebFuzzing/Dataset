package it.pagopa.pn.mandate.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MandateUtilsTest {

    @Test
    void generateRandomCode_shouldReturnFiveDigitString() {
        MandateUtils utils = new MandateUtils();
        String code = utils.generateRandomCode();

        assertNotNull(code);
        assertEquals(5, code.length());
        assertTrue(code.matches("\\d{5}"));
    }

    @Test
    void generateRandomCode_shouldReturnDifferentCodes() {
        MandateUtils utils = new MandateUtils();
        String code1 = utils.generateRandomCode();
        String code2 = utils.generateRandomCode();
        assertNotEquals(code1, code2);
    }

    @Test
    void joinCollection_withValidCollectionToStringAndDelimiter_returnsJoinedString() {
        String result = MandateUtils.joinCollectionToString("-", Arrays.asList("a", "b", "c"));
        assertEquals("a-b-c", result);
    }

    @Test
    void joinCollection_withValidCollectionToStringOfOneElementAndDelimiter_returnsJoinedString() {
        String result = MandateUtils.joinCollectionToString("-", List.of("a"));
        assertEquals("a", result);
    }

    @Test
    void joinCollection_ToString_withNullDelimiter_usesDefaultComma() {
        String result = MandateUtils.joinCollectionToString(null, Arrays.asList("x", "y"));
        assertEquals("x,y", result);
    }

    @Test
    void joinCollection_withNullCollection_ToString_returnsEmptyString() {
        String result = MandateUtils.joinCollectionToString(",", null);
        assertEquals("", result);
    }

    @Test
    void joinCollection_withEmptyCollection_ToString_returnsEmptyString() {
        String result = MandateUtils.joinCollectionToString(",", Collections.emptyList());
        assertEquals("", result);
    }
}
