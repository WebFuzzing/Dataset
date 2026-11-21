package reciter.feedback;

/**
 * Discriminator for how a curator action originated in Publication Manager.
 *
 * <p>Phase 33 D-14: PM UI emits this on every feedback action so the Java
 * write path can distinguish auto-retrieved-candidate curation from
 * curator-driven PubMed search.
 *
 * <ul>
 *   <li>{@link #CANDIDATE_LIST} — curator clicked ACCEPT/REJECT/PENDING on a row in
 *       the auto-retrieved candidate list. The PMID is already (or will be)
 *       attributed via Phase 33-01's retrieval write path.</li>
 *   <li>{@link #PUBMED_SEARCH} — curator queried PubMed inside PM's UI and accepted
 *       (or rejected) a result. The Java write path treats this as its own
 *       retrieval strategy with {@code rs='PM_UI_SEARCH'} (D-13).</li>
 *   <li>{@link #PM_AUTHOR} — curator acted on a row in the PM Authorship Review tab
 *       (the adversarial-attribution-review queue of WCM authorships production buried
 *       or never scored). The Java write path stamps {@code rs='PM_AUTHOR'} into the
 *       ArticleProvenance strategy set so these curations are auditable, but — unlike
 *       PUBMED_SEARCH — it does NOT seed {@code src='PM'}: a never-retrieved authorship
 *       stays {@code src='MAN'} (algo-missed, curator-found) while an already-retrieved
 *       one still lifts to {@code MAN_FROM_PM} via the D-11 transition.</li>
 * </ul>
 */
public enum EntryPath {
    CANDIDATE_LIST,
    PUBMED_SEARCH,
    PM_AUTHOR;

    /**
     * Resolve a string to {@link EntryPath} with case-insensitive matching.
     * Returns {@link #CANDIDATE_LIST} (the default) for null, empty, or unrecognized values.
     */
    public static EntryPath fromString(String s) {
        if (s == null || s.isBlank()) {
            return CANDIDATE_LIST;
        }
        try {
            return EntryPath.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CANDIDATE_LIST;
        }
    }
}
