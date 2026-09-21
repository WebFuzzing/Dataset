package dev.mirodil.testing_system.configs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageableWithFilterResolverTest {
    @InjectMocks
    private PageableWithFilterResolver resolver;
    @Mock
    private NativeWebRequest webRequest;

    @Test
    void testExtractFilterParameters_singleFilter() {
        when(webRequest.getParameterNames()).thenReturn(List.of("scorePercentage>70").iterator());
        when(webRequest.getParameterValues("scorePercentage>70")).thenReturn(new String[]{""});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(1, result.size());
        assertEquals(Map.of(">", "70"), result.get("scorePercentage"));
    }

    @Test
    void testExtractFilterParameters_multipleFilters() {
        when(webRequest.getParameterNames()).thenReturn(List.of("scorePercentage>70", "isPassed").iterator());
        when(webRequest.getParameterValues("scorePercentage>70")).thenReturn(new String[]{""});
        when(webRequest.getParameterValues("isPassed")).thenReturn(new String[]{"true"});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(2, result.size());
        assertEquals(Map.of(">", "70"), result.get("scorePercentage"));
        assertEquals(Map.of("=", "true"), result.get("isPassed"));
    }

    @Test
    void testExtractFilterParameters_singleFilterWithGreaterOrEqualTo() {
        when(webRequest.getParameterNames()).thenReturn(List.of("scorePercentage>").iterator());
        when(webRequest.getParameterValues("scorePercentage>")).thenReturn(new String[]{"70"});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(1, result.size());
        assertEquals(Map.of(">=", "70"), result.get("scorePercentage"));
    }

    @Test
    void testExtractFilterParameters_multipleFiltersWithMixOperators() {
        when(webRequest.getParameterNames()).thenReturn(List.of("scorePercentage>", "eventDatetime<", "isPassed", "whatever<30").iterator());
        when(webRequest.getParameterValues("scorePercentage>")).thenReturn(new String[]{"70"});
        when(webRequest.getParameterValues("eventDatetime<")).thenReturn(new String[]{"2025-03-06"});
        when(webRequest.getParameterValues("isPassed")).thenReturn(new String[]{"true"});
        when(webRequest.getParameterValues("whatever<30")).thenReturn(new String[]{""});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(4, result.size());
        assertEquals(Map.of(">=", "70"), result.get("scorePercentage"));
        assertEquals(Map.of("<=", "2025-03-06"), result.get("eventDatetime"));
        assertEquals(Map.of("=", "true"), result.get("isPassed"));
        assertEquals(Map.of("<", "30"), result.get("whatever"));
    }

    @Test
    void testExtractFilterParameters_multiValueFilterWithEquality() {
        when(webRequest.getParameterNames()).thenReturn(List.of("testEventStatus").iterator());
        when(webRequest.getParameterValues("testEventStatus")).thenReturn(new String[]{"SCHEDULED,RESCHEDULED"});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(1, result.size());
        assertEquals(Map.of("=", "SCHEDULED,RESCHEDULED"), result.get("testEventStatus"));
    }

    @Test
    void testExtractFilterParameters_multiValueFilterWithDifferentOperators() {
        when(webRequest.getParameterNames()).thenReturn(List.of("scorePercentage>", "scorePercentage<90").iterator());
        when(webRequest.getParameterValues("scorePercentage>")).thenReturn(new String[]{"70"});
        when(webRequest.getParameterValues("scorePercentage<90")).thenReturn(new String[]{""});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(1, result.size());
        assertEquals(2, result.get("scorePercentage").size());
        assertEquals(Map.of(">=", "70", "<", "90"), result.get("scorePercentage"));
    }

    @Test
    void testExtractFilterParameters_multiValueFilterWithSameOperator_shouldOverwrite() {
        when(webRequest.getParameterNames()).thenReturn(List.of("scorePercentage>70", "scorePercentage>90").iterator());
        when(webRequest.getParameterValues("scorePercentage>70")).thenReturn(new String[]{""});
        when(webRequest.getParameterValues("scorePercentage>90")).thenReturn(new String[]{""});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(1, result.size());
        assertEquals(1, result.get("scorePercentage").size());
        assertEquals(Map.of(">", "90"), result.get("scorePercentage"));
    }

    @Test
    void testExtractFilterParameters_paginationAndSortParams_shouldIgnore() {
        when(webRequest.getParameterNames()).thenReturn(List.of("page", "size", "isPassed", "sort").iterator());
        when(webRequest.getParameterValues("isPassed")).thenReturn(new String[]{"null"});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(1, result.size());
        assertEquals(Map.of("=", "null"), result.get("isPassed"));
    }

    @Test
    void testExtractFilterParameters_spaces() {
        when(webRequest.getParameterNames()).thenReturn(List.of("title", "isPassed> true ", " whatever <").iterator());
        when(webRequest.getParameterValues("title")).thenReturn(new String[]{" New test "});
        when(webRequest.getParameterValues("isPassed> true ")).thenReturn(new String[]{""});
        when(webRequest.getParameterValues(" whatever <")).thenReturn(new String[]{" 50 "});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(3, result.size());
        assertEquals(Map.of("=", "New test"), result.get("title"));
        assertEquals(Map.of(">", "true"), result.get("isPassed"));
        assertEquals(Map.of("<=", "50"), result.get("whatever"));
    }

    @Test
    void testExtractFilterParameters_missingValue_shouldIgnore() {
        when(webRequest.getParameterNames()).thenReturn(List.of("title").iterator());
        when(webRequest.getParameterValues("title")).thenReturn(new String[]{""});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(0, result.size());
    }

    @Test
    void testExtractFilterParameters_malformedOperators() {
        when(webRequest.getParameterNames()).thenReturn(List.of("score>>50", "isPassed", "scorePercentage>", "whatever>", "whatever!").iterator());
        when(webRequest.getParameterValues("score>>50")).thenReturn(new String[]{""});
        when(webRequest.getParameterValues("isPassed")).thenReturn(new String[]{">true"});
        when(webRequest.getParameterValues("scorePercentage>")).thenReturn(new String[]{"70,90"});
        when(webRequest.getParameterValues("whatever>")).thenReturn(new String[]{">50"});
        when(webRequest.getParameterValues("whatever!")).thenReturn(new String[]{"50"});

        Map<String, Map<String, String>> result = resolver.extractFilterParameters(webRequest);

        assertEquals(5, result.size());
        assertEquals(Map.of(">", ">50"), result.get("score")); // ?score>>50
        assertEquals(Map.of("=", ">true"), result.get("isPassed")); // ?isPassed=>true
        assertEquals(Map.of(">=", "70,90"), result.get("scorePercentage")); // ?scorePercentage>=70,90
        assertEquals(Map.of(">=", ">50"), result.get("whatever")); // ?whatever>=>50
        assertEquals(Map.of("=", "50"), result.get("whatever!")); // ?whatever!=50
    }
}

