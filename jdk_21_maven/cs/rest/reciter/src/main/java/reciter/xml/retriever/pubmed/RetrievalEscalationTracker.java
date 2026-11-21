package reciter.xml.retriever.pubmed;

import reciter.api.parameters.RetrievalRefreshFlag;

/**
 * Thread-scoped record of what retrieval a request actually performed, so the
 * controller can report it back to the caller (#696).
 *
 * The batch client budgets ALL_PUBLICATIONS escalations per night, but the escalation
 * decision is made server-side, deep inside {@code retrieveArticlesByUid} — the client
 * cannot observe it from the response body, which is the feature-generator's scoring
 * payload. This tracker carries three facts from the decision points up to the HTTP
 * response, where the controller emits them as headers:
 *
 * <ul>
 *   <li>{@code X-Reciter-Retrieval-Mode} — the retrieval mode actually executed
 *       ({@code ALL_PUBLICATIONS}, {@code ONLY_NEWLY_ADDED_PUBLICATIONS}, or
 *       {@code NONE} when the request used cached results);</li>
 *   <li>{@code X-Reciter-Escalated} — true iff an ONLY_NEWLY_ADDED request was
 *       escalated to ALL_PUBLICATIONS by the {@code fullSweepMaxAgeDays} staleness
 *       check. This is the value the client counts against its budget;</li>
 *   <li>{@code X-Reciter-Auto-Upgraded} — true iff the request was forced to
 *       ALL_PUBLICATIONS because the uid had no ESearchResult at all. Counted
 *       separately because those sweeps are unbudgeted: during any recurrence of the
 *       S3-offload null-read behavior they can number in the thousands, and folding
 *       them into the escalation count would make "the budget is holding" a false
 *       signal.</li>
 * </ul>
 *
 * The decision points all run on the servlet thread handling the request (the engine
 * fans retrieval out to a worker pool but the controller blocks on it), so a
 * ThreadLocal scopes these flags to one request — same reasoning as
 * {@link RetrievalErrorTracker}, which scopes the per-run PubMed-failure flag to one
 * retrieval worker thread. {@link #reset()} runs at endpoint entry, which also guards
 * against servlet-thread reuse.
 */
public final class RetrievalEscalationTracker {

    private static final ThreadLocal<Boolean> ESCALATED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> AUTO_UPGRADED = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<RetrievalRefreshFlag> MODE = new ThreadLocal<>();

    private RetrievalEscalationTracker() {
    }

    /** Clear all flags at the start of a request-handling endpoint. */
    public static void reset() {
        ESCALATED.set(Boolean.FALSE);
        AUTO_UPGRADED.set(Boolean.FALSE);
        MODE.remove();
    }

    /** An ONLY_NEWLY_ADDED request was escalated to ALL_PUBLICATIONS by the staleness check. */
    public static void markEscalated() {
        ESCALATED.set(Boolean.TRUE);
    }

    /** The request was forced to ALL_PUBLICATIONS because the uid had no ESearchResult. */
    public static void markAutoUpgraded() {
        AUTO_UPGRADED.set(Boolean.TRUE);
    }

    /** Record the retrieval mode actually dispatched to the engine. */
    public static void setMode(RetrievalRefreshFlag mode) {
        MODE.set(mode);
    }

    public static boolean wasEscalated() {
        return ESCALATED.get();
    }

    public static boolean wasAutoUpgraded() {
        return AUTO_UPGRADED.get();
    }

    /** Mode actually executed, or null when the request performed no retrieval. */
    public static RetrievalRefreshFlag getMode() {
        return MODE.get();
    }
}
