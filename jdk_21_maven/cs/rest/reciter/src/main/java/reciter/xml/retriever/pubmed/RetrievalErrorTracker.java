package reciter.xml.retriever.pubmed;

/**
 * Thread-scoped "this retrieval run hit a PubMed/REST failure" flag.
 *
 * ONLY_NEWLY_ADDED retrieval advances the ESearchResult.retrievalDate watermark on every run.
 * When a PubMed call is swallowed (tool 500 / NCBI 429 → empty result), the run looks like
 * "no new papers" and the watermark still advances, so the missed window is never re-scanned
 * and the article is lost until a full ALL_PUBLICATIONS re-scan (see ReCiter#689).
 *
 * Retrieval for a single uid runs synchronously on one AsyncRetrievalEngine thread, so a
 * ThreadLocal cleanly scopes the flag to that run: the engine calls {@link #reset()} before a
 * run and checks {@link #hadError()} after, rolling the watermark back on failure. Fail-safe —
 * a stale or unset flag at worst triggers an extra (idempotent) re-scan, never data loss.
 */
public final class RetrievalErrorTracker {

    private static final ThreadLocal<Boolean> ERROR = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private RetrievalErrorTracker() {
    }

    /** Clear the flag at the start of a retrieval run (also guards against thread reuse). */
    public static void reset() {
        ERROR.set(Boolean.FALSE);
    }

    /** Record that a PubMed/REST call failed during the current run. */
    public static void markError() {
        ERROR.set(Boolean.TRUE);
    }

    /** True if any PubMed/REST call failed since the last {@link #reset()} on this thread. */
    public static boolean hadError() {
        return ERROR.get();
    }
}
