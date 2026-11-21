package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.exceptions.PnForbiddenException;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PgUtilsTest {

    @Test
    void testValidaAccessoPF() {
        StepVerifier.create(PgUtils.validaAccessoOnlyAdmin(CxTypeAuthFleet.PF, null, null))
                .expectNextCount(1)
                .expectComplete();
        StepVerifier.create(PgUtils.validaAccessoOnlyAdmin(null, null, null))
                .expectNextCount(1)
                .expectComplete();
    }

    @Test
    void testValidaAccessoPG() {
        StepVerifier.create(PgUtils.validaAccessoOnlyAdmin(CxTypeAuthFleet.PG, "admin", null))
                .expectNextCount(1)
                .expectComplete();
        StepVerifier.create(PgUtils.validaAccessoOnlyAdmin(CxTypeAuthFleet.PG, "admin", Collections.emptyList()))
                .expectNextCount(1)
                .expectComplete();
        StepVerifier.create(PgUtils.validaAccessoOnlyGroupAdmin(CxTypeAuthFleet.PG, "admin", List.of("")))
                .expectNextCount(1)
                .expectComplete();
    }

    @Test
    void testValidaAccessoPG_Failure() {
        StepVerifier.create(PgUtils.validaAccessoOnlyAdmin(CxTypeAuthFleet.PG, "operator", null))
                .expectError(PnForbiddenException.class)
                .verify();
        StepVerifier.create(PgUtils.validaAccessoOnlyAdmin(CxTypeAuthFleet.PG, "admin", List.of("")))
                .expectError(PnForbiddenException.class)
                .verify();
        StepVerifier.create(PgUtils.validaAccessoOnlyGroupAdmin(CxTypeAuthFleet.PG, "operator", List.of("")))
                .expectError(PnForbiddenException.class)
                .verify();
    }

    @Test
    void testBuildExpressionGroupFilter1() {
        Map<String, AttributeValue> attributeValue = new HashMap<>();
        String result = PgUtils.buildExpressionGroupFilter(List.of("G1"), attributeValue);
        assertEquals("( contains("+ MandateEntity.COL_A_GROUPS +",:group0) )", result);
        assertEquals(1, attributeValue.size());
    }

    @Test
    void testBuildExpressionGroupFilter2() {
        Map<String, AttributeValue> attributeValue = new HashMap<>();
        String result = PgUtils.buildExpressionGroupFilter(List.of("G1", "G2"), attributeValue);
        assertEquals("( contains("+MandateEntity.COL_A_GROUPS+",:group0) OR contains("+MandateEntity.COL_A_GROUPS+",:group1) )", result);
        assertEquals(2, attributeValue.size());
    }

    @Test
    @DisplayName("Test groups empty")
    void testGetGroupsForSecureFilter1() {
        List<String> cxGroups = List.of("G");
        assertEquals(cxGroups, PgUtils.getGroupsForSecureFilter(null, cxGroups));
        assertEquals(cxGroups, PgUtils.getGroupsForSecureFilter(Collections.emptyList(), cxGroups));
    }

    @Test
    @DisplayName("Test cx-groups empty")
    void testGetGroupsForSecureFilter2() {
        List<String> groups = List.of("G");
        assertEquals(groups, PgUtils.getGroupsForSecureFilter(groups, null));
        assertEquals(groups, PgUtils.getGroupsForSecureFilter(groups, Collections.emptyList()));
    }

    @Test
    @DisplayName("Test both empty")
    void testGetGroupsForSecureFilter3() {
        assertTrue(PgUtils.getGroupsForSecureFilter(null, null).isEmpty());
        assertTrue(PgUtils.getGroupsForSecureFilter(Collections.emptyList(), Collections.emptyList()).isEmpty());
    }

    @Test
    @DisplayName("Test groups and cx-groups")
    void testGetGroupsForSecureFilter4() {
        List<String> groups = List.of("G1");
        List<String> cxGroups = List.of("G1", "G2");
        assertEquals(groups, PgUtils.getGroupsForSecureFilter(groups, cxGroups));
    }

    @Test
    @DisplayName("Test groups and cx-groups")
    void testGetGroupsForSecureFilter5() {
        List<String> groups = List.of("G3");
        List<String> cxGroups = List.of("G1", "G2");
        assertThrows(PnForbiddenException.class, () -> PgUtils.getGroupsForSecureFilter(groups, cxGroups));
    }
}