package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reciter.database.dynamodb.model.ExternalArticle;
import reciter.engine.analysis.ReCiterArticleFeature;

/**
 * Duplicate detection for manually added external-source articles.
 *
 * BLOCKED: the work already exists in the person's PubMed-domain record (gold
 * standard or retrieved candidates, matched by PMID or DOI) or was already added
 * from another external source. No override.
 *
 * WARNING: a fuzzy title+year collision. Overridable with force=true at the API.
 */
public final class ExternalArticleDupCheck {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");
    /** doi.org / dx.doi.org URL forms and the bare "doi:" prefix all denote the same DOI. */
    private static final Pattern DOI_PREFIX = Pattern.compile("^(https?://(dx\\.)?doi\\.org/|doi:\\s*)");

    public enum Level {
        OK, WARNING, BLOCKED
    }

    /**
     * Why a match fired. Serializes to its own name, so the JSON contract PM reads is
     * unchanged from when this was a String.
     *
     * Note that {@link Match#getMatchedId()} is polymorphic across these: the
     * *_EXTERNAL and ALREADY_ADDED types carry an ExternalArticle articleId
     * (e.g. "SCOPUS:2-s2.0-..."), every other type carries a PMID. See #735.
     */
    public enum MatchType {
        ALREADY_ADDED,
        PMID_MATCH_EXTERNAL,
        DOI_MATCH_EXTERNAL,
        TITLE_YEAR_MATCH_EXTERNAL,
        PMID_IN_GOLD_STANDARD,
        PMID_REJECTED_IN_GOLD_STANDARD,
        PMID_IN_CANDIDATES,
        DOI_MATCH,
        TITLE_YEAR_MATCH
    }

    public static final class Match {
        private final MatchType type;
        private final String matchedId;
        private final String detail;
        private final String title;
        private final String journal;
        private final String pubYear;

        Match(MatchType type, String matchedId, String detail) {
            this(type, matchedId, detail, null, null, null);
        }

        Match(MatchType type, String matchedId, String detail, String title, String journal, String pubYear) {
            this.type = type;
            this.matchedId = matchedId;
            this.detail = detail;
            this.title = title;
            this.journal = journal;
            this.pubYear = pubYear;
        }

        public MatchType getType() {
            return type;
        }

        public String getMatchedId() {
            return matchedId;
        }

        public String getDetail() {
            return detail;
        }

        /** Matched record's title, when known — null for the two gold-standard PMID
         * matches (a GoldStandard entry carries no title, only the PMID). */
        public String getTitle() {
            return title;
        }

        /** Matched record's journal/venue, when known — null where the underlying
         * record doesn't carry one (e.g. gold-standard PMID matches). */
        public String getJournal() {
            return journal;
        }

        /** Matched record's publication year, when known. */
        public String getPubYear() {
            return pubYear;
        }
    }

    public static final class Result {
        private final Level level;
        private final List<Match> matches;

        Result(Level level, List<Match> matches) {
            this.level = level;
            this.matches = List.copyOf(matches);
        }

        public Level getLevel() {
            return level;
        }

        public List<Match> getMatches() {
            return matches;
        }
    }

    private ExternalArticleDupCheck() {
    }

    public static Result check(ExternalArticle candidate,
                               List<Long> knownPmids,
                               List<Long> rejectedPmids,
                               List<ReCiterArticleFeature> candidateArticles,
                               List<ExternalArticle> existingExternal) {
        List<Match> blocked = new ArrayList<>();
        List<Match> warnings = new ArrayList<>();
        String candidateDoi = normalizeDoi(candidate.getDoi());
        String candidateTitle = normalizeTitle(candidate.getTitle());
        String candidateYear = year(candidate.getPubDate());

        for (ExternalArticle existing : safe(existingExternal)) {
            String existingYear = year(existing.getPubDate());
            if (existing.getArticleId() != null && existing.getArticleId().equals(candidate.getArticleId())) {
                blocked.add(new Match(MatchType.ALREADY_ADDED, existing.getArticleId(),
                        "This external article was already added for this person.",
                        existing.getTitle(), existing.getJournalOrVenue(), existingYear));
            } else if (candidate.getPmid() != null && candidate.getPmid().equals(existing.getPmid())) {
                blocked.add(new Match(MatchType.PMID_MATCH_EXTERNAL, existing.getArticleId(),
                        "An external article with the same PMID was already added from " + existing.getSourceType() + ".",
                        existing.getTitle(), existing.getJournalOrVenue(), existingYear));
            } else if (candidateDoi != null && candidateDoi.equals(normalizeDoi(existing.getDoi()))) {
                blocked.add(new Match(MatchType.DOI_MATCH_EXTERNAL, existing.getArticleId(),
                        "An external article with the same DOI was already added from " + existing.getSourceType() + ".",
                        existing.getTitle(), existing.getJournalOrVenue(), existingYear));
            } else if (titleYearCollision(candidateTitle, candidateYear,
                    normalizeTitle(existing.getTitle()), existingYear)) {
                warnings.add(new Match(MatchType.TITLE_YEAR_MATCH_EXTERNAL, existing.getArticleId(),
                        "An external article with a matching title and year was already added: " + existing.getTitle(),
                        existing.getTitle(), existing.getJournalOrVenue(), existingYear));
            }
        }

        if (candidate.getPmid() != null && knownPmids != null && knownPmids.contains(candidate.getPmid())) {
            blocked.add(new Match(MatchType.PMID_IN_GOLD_STANDARD, String.valueOf(candidate.getPmid()),
                    "This PMID is already accepted in the person's gold standard."));
        }
        if (candidate.getPmid() != null && rejectedPmids != null && rejectedPmids.contains(candidate.getPmid())) {
            blocked.add(new Match(MatchType.PMID_REJECTED_IN_GOLD_STANDARD, String.valueOf(candidate.getPmid()),
                    "This PMID was explicitly rejected by a curator; do not re-add it from an external source."));
        }

        for (ReCiterArticleFeature feature : safe(candidateArticles)) {
            String featureYear = year(feature.getPublicationDateStandardized());
            if (candidate.getPmid() != null && candidate.getPmid().longValue() == feature.getPmid()) {
                blocked.add(new Match(MatchType.PMID_IN_CANDIDATES, String.valueOf(feature.getPmid()),
                        "This PMID exists in the person's PubMed candidate set; adjudicate it via normal feedback instead.",
                        feature.getArticleTitle(), feature.getJournalTitleVerbose(), featureYear));
            } else if (candidateDoi != null && candidateDoi.equals(normalizeDoi(feature.getDoi()))) {
                blocked.add(new Match(MatchType.DOI_MATCH, String.valueOf(feature.getPmid()),
                        "A PubMed article with the same DOI exists in the person's candidate set (PMID " + feature.getPmid() + ").",
                        feature.getArticleTitle(), feature.getJournalTitleVerbose(), featureYear));
            } else if (titleYearCollision(candidateTitle, candidateYear,
                    normalizeTitle(feature.getArticleTitle()), featureYear)) {
                warnings.add(new Match(MatchType.TITLE_YEAR_MATCH, String.valueOf(feature.getPmid()),
                        "A PubMed article with a matching title and year exists (PMID " + feature.getPmid() + "): " + feature.getArticleTitle(),
                        feature.getArticleTitle(), feature.getJournalTitleVerbose(), featureYear));
            }
        }

        if (!blocked.isEmpty()) {
            blocked.addAll(warnings);
            return new Result(Level.BLOCKED, blocked);
        }
        if (!warnings.isEmpty()) {
            return new Result(Level.WARNING, warnings);
        }
        return new Result(Level.OK, new ArrayList<>());
    }

    /**
     * Supersede matching (#660): returns the accepted PMID that supersedes this
     * external row — by direct PMID first, then by normalized DOI — or null if
     * none. acceptedDoiToPmid must be keyed by {@link #normalizeDoi} output.
     */
    public static Long matchSupersedingPmid(ExternalArticle row,
                                            Set<Long> acceptedPmids,
                                            Map<String, Long> acceptedDoiToPmid) {
        if (row.getPmid() != null && acceptedPmids != null && acceptedPmids.contains(row.getPmid())) {
            return row.getPmid();
        }
        String rowDoi = normalizeDoi(row.getDoi());
        if (rowDoi == null || acceptedDoiToPmid == null) {
            return null;
        }
        return acceptedDoiToPmid.get(rowDoi);
    }

    // ponytail: exact match on normalized title + same year. Upgrade to token-overlap
    // similarity if real-world misses (subtitle variants, punctuation-heavy titles) show up.
    private static boolean titleYearCollision(String titleA, String yearA, String titleB, String yearB) {
        if (titleA == null || titleB == null || !titleA.equals(titleB)) {
            return false;
        }
        // Identical titles collide unless both years are known and differ.
        return yearA == null || yearB == null || yearA.equals(yearB);
    }

    /** Lowercase, trim, strip any doi.org URL or "doi:" prefix. Returns null for blank input. */
    static String normalizeDoi(String doi) {
        if (doi == null) {
            return null;
        }
        String normalized = DOI_PREFIX.matcher(doi.trim().toLowerCase(Locale.ROOT))
                .replaceFirst("").trim();
        return normalized.isBlank() ? null : normalized;
    }

    /** Lowercase, collapse all non-alphanumerics to single spaces. Returns null for blank input. */
    static String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        String normalized = title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.isBlank() ? null : normalized;
    }

    /** First 4-digit year found in a date string, or null. */
    static String year(String date) {
        if (date == null) {
            return null;
        }
        Matcher m = YEAR.matcher(date);
        return m.find() ? m.group() : null;
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }
}
