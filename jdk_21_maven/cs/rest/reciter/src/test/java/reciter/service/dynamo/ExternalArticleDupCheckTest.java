package reciter.service.dynamo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import reciter.database.dynamodb.model.ExternalArticle;
import reciter.engine.analysis.ReCiterArticleFeature;
import reciter.service.dynamo.ExternalArticleDupCheck.Level;
import reciter.service.dynamo.ExternalArticleDupCheck.MatchType;
import reciter.service.dynamo.ExternalArticleDupCheck.Result;

public class ExternalArticleDupCheckTest {

    private ExternalArticle candidate(String articleId, Long pmid, String doi, String title, String pubDate) {
        ExternalArticle externalArticle = new ExternalArticle();
        externalArticle.setUid("test1234");
        externalArticle.setArticleId(articleId);
        externalArticle.setPmid(pmid);
        externalArticle.setDoi(doi);
        externalArticle.setTitle(title);
        externalArticle.setPubDate(pubDate);
        return externalArticle;
    }

    private ReCiterArticleFeature pubmedArticle(long pmid, String doi, String title, String dateStandardized) {
        ReCiterArticleFeature feature = new ReCiterArticleFeature();
        feature.setPmid(pmid);
        feature.setDoi(doi);
        feature.setArticleTitle(title);
        feature.setPublicationDateStandardized(dateStandardized);
        return feature;
    }

    @Test
    public void cleanAddIsOk() {
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W123", null, "10.1000/xyz", "A Novel Finding", "2025-06-01"),
                Arrays.asList(11111L),
                Collections.emptyList(),
                Arrays.asList(pubmedArticle(11111L, "10.1000/abc", "Something Else Entirely", "2020-01-01")),
                Collections.emptyList());
        assertEquals(Level.OK, result.getLevel());
    }

    @Test
    public void pmidInGoldStandardBlocks() {
        Result result = ExternalArticleDupCheck.check(
                candidate("SCOPUS:850001", 11111L, null, "Any Title", "2020"),
                Arrays.asList(11111L),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
        assertEquals(Level.BLOCKED, result.getLevel());
        assertEquals(MatchType.PMID_IN_GOLD_STANDARD, result.getMatches().get(0).getType());
    }

    @Test
    public void rejectedPmidBlocks() {
        Result result = ExternalArticleDupCheck.check(
                candidate("SCOPUS:850001", 99999L, null, "Any Title", "2020"),
                Collections.emptyList(),
                Arrays.asList(99999L),
                Collections.emptyList(),
                Collections.emptyList());
        assertEquals(Level.BLOCKED, result.getLevel());
        assertEquals(MatchType.PMID_REJECTED_IN_GOLD_STANDARD, result.getMatches().get(0).getType());
    }

    @Test
    public void pmidInCandidateSetBlocks() {
        Result result = ExternalArticleDupCheck.check(
                candidate("SCOPUS:850001", 22222L, null, "Any Title", "2020"),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(pubmedArticle(22222L, null, "Any Title", "2020-01-01")),
                Collections.emptyList());
        assertEquals(Level.BLOCKED, result.getLevel());
        assertEquals(MatchType.PMID_IN_CANDIDATES, result.getMatches().get(0).getType());
    }

    @Test
    public void pmidMatchAgainstExistingExternalBlocks() {
        ExternalArticle existing = candidate("SCOPUS:850002", 88888L, null, "From Scopus", "2022");
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W456", 88888L, null, "From OpenAlex", "2022"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(existing));
        assertEquals(Level.BLOCKED, result.getLevel());
        assertEquals(MatchType.PMID_MATCH_EXTERNAL, result.getMatches().get(0).getType());
    }

    @Test
    public void doiMatchAgainstCandidatesBlocksDespiteUrlPrefixAndCase() {
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W123", null, "https://doi.org/10.1000/ABC", "Different Title", "2021"),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(pubmedArticle(33333L, "10.1000/abc", "Original Title", "2021-05-01")),
                Collections.emptyList());
        assertEquals(Level.BLOCKED, result.getLevel());
        assertEquals(MatchType.DOI_MATCH, result.getMatches().get(0).getType());
    }

    @Test
    public void doiMatchAgainstExistingExternalBlocks() {
        ExternalArticle existing = candidate("SCOPUS:850002", null, "10.1000/xyz", "Same Work From Scopus", "2022");
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W456", null, "10.1000/xyz", "Same Work From OpenAlex", "2022"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(existing));
        assertEquals(Level.BLOCKED, result.getLevel());
        assertEquals(MatchType.DOI_MATCH_EXTERNAL, result.getMatches().get(0).getType());
    }

    @Test
    public void sameArticleIdBlocks() {
        ExternalArticle existing = candidate("OPENALEX:W123", null, null, "A Title", "2020");
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W123", null, null, "A Title", "2020"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(existing));
        assertEquals(Level.BLOCKED, result.getLevel());
        ExternalArticleDupCheck.Match match = result.getMatches().get(0);
        assertEquals(MatchType.ALREADY_ADDED, match.getType());
        assertEquals("A Title", match.getTitle());
        assertEquals("2020", match.getPubYear());
    }

    @Test
    public void titleAndYearCollisionWarns() {
        ReCiterArticleFeature matched = pubmedArticle(44444L, null, "Deep learning for author disambiguation", "2023-09-15");
        matched.setJournalTitleVerbose("Journal of Disambiguation");
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W123", null, null, "Deep Learning for Author Disambiguation!", "2023"),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(matched),
                Collections.emptyList());
        assertEquals(Level.WARNING, result.getLevel());
        ExternalArticleDupCheck.Match match = result.getMatches().get(0);
        assertEquals(MatchType.TITLE_YEAR_MATCH, match.getType());
        assertEquals("Deep learning for author disambiguation", match.getTitle());
        assertEquals("Journal of Disambiguation", match.getJournal());
        assertEquals("2023", match.getPubYear());
    }

    @Test
    public void titleCollisionWithOneMissingYearStillWarns() {
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W123", null, null, "Deep Learning for Author Disambiguation", "2023"),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(pubmedArticle(44444L, null, "Deep learning for author disambiguation", null)),
                Collections.emptyList());
        assertEquals(Level.WARNING, result.getLevel());
        assertEquals(MatchType.TITLE_YEAR_MATCH, result.getMatches().get(0).getType());
    }

    @Test
    public void sameTitleDifferentYearIsOk() {
        Result result = ExternalArticleDupCheck.check(
                candidate("OPENALEX:W123", null, null, "Annual Report on Disambiguation", "2024"),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(pubmedArticle(55555L, null, "Annual Report on Disambiguation", "2023-01-01")),
                Collections.emptyList());
        assertEquals(Level.OK, result.getLevel());
    }

    @Test
    public void blockedOutranksWarning() {
        List<ReCiterArticleFeature> candidates = Arrays.asList(
                pubmedArticle(66666L, "10.1000/dup", "A Fuzzy Title Match", "2022-01-01"),
                pubmedArticle(77777L, null, "A Fuzzy Title Match", "2022-03-01"));
        Result result = ExternalArticleDupCheck.check(
                candidate("SCOPUS:850003", null, "10.1000/dup", "A Fuzzy Title Match", "2022"),
                Collections.emptyList(),
                Collections.emptyList(),
                candidates,
                Collections.emptyList());
        assertEquals(Level.BLOCKED, result.getLevel());
    }

    @Test
    public void supersedeMatchesByPmidFirst() {
        ExternalArticle row = candidate("SCOPUS:850010", 12345L, "10.1000/xyz", "A Title", "2024");
        Long superseding = ExternalArticleDupCheck.matchSupersedingPmid(
                row, Collections.singleton(12345L), Collections.emptyMap());
        assertEquals(Long.valueOf(12345L), superseding);
    }

    @Test
    public void supersedeMatchesByNormalizedDoi() {
        ExternalArticle row = candidate("OPENALEX:W789", null, "https://doi.org/10.1000/XYZ", "A Title", "2024");
        java.util.Map<String, Long> acceptedDoiToPmid = new java.util.HashMap<>();
        acceptedDoiToPmid.put("10.1000/xyz", 55555L);
        Long superseding = ExternalArticleDupCheck.matchSupersedingPmid(
                row, Collections.singleton(55555L), acceptedDoiToPmid);
        assertEquals(Long.valueOf(55555L), superseding);
    }

    @Test
    public void supersedeNoMatchReturnsNull() {
        java.util.Map<String, Long> acceptedDoiToPmid = new java.util.HashMap<>();
        acceptedDoiToPmid.put("10.1000/xyz", 55555L);
        assertNull(ExternalArticleDupCheck.matchSupersedingPmid(
                candidate("OPENALEX:W789", null, "10.1000/other", "A Title", "2024"),
                Collections.singleton(55555L), acceptedDoiToPmid));
        assertNull(ExternalArticleDupCheck.matchSupersedingPmid(
                candidate("WOS:123", null, null, "No DOI", "2024"),
                Collections.singleton(55555L), acceptedDoiToPmid));
    }

    @Test
    public void normalizers() {
        assertEquals("10.1000/abc", ExternalArticleDupCheck.normalizeDoi(" https://dx.doi.org/10.1000/ABC "));
        assertNull(ExternalArticleDupCheck.normalizeDoi("  "));
    }

    /** Every form a curator or an upstream source can hand us for the same DOI must
     * normalize to one string, or the BLOCKED DOI paths silently miss real duplicates. */
    @Test
    public void normalizeDoiFoldsEveryCommonPrefixForm() {
        String expected = "10.1000/abc";
        for (String variant : Arrays.asList(
                "10.1000/abc",
                "10.1000/ABC",
                "  10.1000/abc  ",
                "doi:10.1000/abc",
                "DOI: 10.1000/abc",
                "http://doi.org/10.1000/abc",
                "https://doi.org/10.1000/abc",
                "http://dx.doi.org/10.1000/abc",
                "https://dx.doi.org/10.1000/ABC")) {
            assertEquals(variant, expected, ExternalArticleDupCheck.normalizeDoi(variant));
        }
        assertNull(ExternalArticleDupCheck.normalizeDoi(null));
        assertNull(ExternalArticleDupCheck.normalizeDoi("https://doi.org/"));
    }

    /** A "doi:"-prefixed external DOI must still BLOCK against the bare form in the
     * person's PubMed candidate set — the case the folding above exists for. */
    @Test
    public void doiPrefixVariantStillBlocksAgainstCandidateSet() {
        Result result = ExternalArticleDupCheck.check(
                candidate("SCOPUS:1", null, "doi: 10.1000/ABC", "Some title", "2024"),
                null, null,
                Collections.singletonList(pubmedArticle(111L, "10.1000/abc", "Other title", "2024-01-01")),
                Collections.emptyList());
        assertEquals(Level.BLOCKED, result.getLevel());
        assertEquals(MatchType.DOI_MATCH, result.getMatches().get(0).getType());
        assertEquals("deep learning 2 0", ExternalArticleDupCheck.normalizeTitle("Deep-Learning: 2.0?"));
        assertEquals("2023", ExternalArticleDupCheck.year("2023-09-15"));
        assertNull(ExternalArticleDupCheck.year("n.d."));
    }

    /** A CROSSREF row stays CROSSREF forever, so the same DOI arriving from another source must
     * BLOCK on DOI — in both directions (#1060 in PM). */
    @Test
    public void crossrefAndOpenAlexSameDoiBlockBothWays() {
        ExternalArticle crossref = candidate("CROSSREF:10.1016/j.surg.2026.110815", null,
                "10.1016/j.surg.2026.110815", "Crossref Title", "2026");
        ExternalArticle openalex = candidate("OPENALEX:W999", null,
                "https://doi.org/10.1016/J.SURG.2026.110815", "OpenAlex Title", "2026");

        Result crossrefAfterOpenAlex = ExternalArticleDupCheck.check(crossref,
                null, null, null, Collections.singletonList(openalex));
        assertEquals(Level.BLOCKED, crossrefAfterOpenAlex.getLevel());
        assertEquals(MatchType.DOI_MATCH_EXTERNAL, crossrefAfterOpenAlex.getMatches().get(0).getType());
        assertEquals("OPENALEX:W999", crossrefAfterOpenAlex.getMatches().get(0).getMatchedId());

        Result openAlexAfterCrossref = ExternalArticleDupCheck.check(openalex,
                null, null, null, Collections.singletonList(crossref));
        assertEquals(Level.BLOCKED, openAlexAfterCrossref.getLevel());
        assertEquals(MatchType.DOI_MATCH_EXTERNAL, openAlexAfterCrossref.getMatches().get(0).getType());
        assertEquals("CROSSREF:10.1016/j.surg.2026.110815", openAlexAfterCrossref.getMatches().get(0).getMatchedId());
    }
}
