package reciter.service;

import reciter.feedback.EntryPath;

/**
 * Writes provenance records to the {@code ArticleProvenance} DynamoDB table.
 *
 * <p>ArticleProvenance has the schema {@code (uid: S [PK], articleId: S [SK])}
 * with attributes:
 * <ul>
 *   <li>{@code rs}  — primary retrieval strategy code (first strategy to find this PMID for this uid)</li>
 *   <li>{@code frd} — first retrieval date, epoch seconds</li>
 *   <li>{@code ads} — additional strategy codes (String Set), grows as new strategies re-find this PMID</li>
 *   <li>{@code src} — provenance source: PM, MAN, CTSC, MAN_FROM_PM, MAN_FROM_CTSC (owned by curator/CTSC paths, not retrieval)</li>
 * </ul>
 *
 * <p>This service writes only the retrieval-side fields ({@code rs}, {@code frd}, {@code ads}).
 * It uses raw {@code AmazonDynamoDB.updateItem} (not DynamoDBMapper) to access expression
 * features ({@code if_not_exists}, {@code ADD} on String Sets) that DynamoDBMapper does
 * not support cleanly. Phase 33 D-02/D-03/D-04 govern these semantics.
 */
public interface ArticleProvenanceService {

    /**
     * Upsert retrieval provenance for one (uid, pmid) pair.
     *
     * <p>Behavior:
     * <ul>
     *   <li>{@code rs} is set to {@code strategyCode} only if {@code rs} is currently absent
     *       (first strategy wins; preserves original retrieval attribution on re-runs).</li>
     *   <li>{@code frd} is set to {@code epochSeconds} only if currently absent (preserves
     *       the original first-retrieval-date).</li>
     *   <li>{@code ads} always grows: {@code strategyCode} is ADDed to the String Set.</li>
     *   <li>{@code src} is NOT touched (owned by curator/CTSC paths per Phase 33 D-04).</li>
     * </ul>
     *
     * <p>Errors are logged but never thrown — provenance write failures must not break
     * retrieval. Callers can fire-and-forget.
     *
     * @param uid          person identifier
     * @param pmid         PubMed ID
     * @param strategyCode retrieval strategy code (e.g. {@code "PUBMED_NAME_AFFIL"})
     * @param epochSeconds first-retrieval-date as epoch seconds (typically {@code now})
     */
    void upsertRetrievalProvenance(String uid, long pmid, String strategyCode, long epochSeconds);

    /**
     * Upsert provenance for a curator action (Phase 33 D-11 / D-13).
     *
     * <p>Implements the unified D-11 transition rule: regardless of feedback value
     * (ACCEPTED, REJECTED, PENDING), the {@code src} field is updated according to
     * the existing src per the table:
     *
     * <pre>
     *   existing src              →  new src
     *   ──────────────────────────────────────────
     *   absent or 'GS'            →  'MAN'
     *   'PM'                      →  'MAN_FROM_PM'
     *   'CTSC'                    →  'MAN_FROM_CTSC'
     *   'MAN' / 'MAN_FROM_*'      →  unchanged (no-op)
     * </pre>
     *
     * <p>{@code frd} is preserved if set; otherwise written as {@code epochSeconds}.
     *
     * <p>If {@code entryPath == PUBMED_SEARCH} (D-13), an additional retrieval-style
     * write happens FIRST: {@code rs = if_not_exists(rs, 'PM_UI_SEARCH')},
     * {@code src = if_not_exists(src, 'PM')}, {@code ADD ads :PM_UI_SEARCH_set}.
     * This ensures the curator-search-via-PubMed origin is recorded; the subsequent
     * D-11 transition then lifts {@code src=PM} to {@code MAN_FROM_PM}.
     *
     * <p>Implementation uses GetItem-then-conditional-UpdateItem with one retry on
     * concurrent-write CCE; if the second attempt also fails, log WARN and continue
     * (D-12). Provenance writes never block PM UI requests.
     *
     * @param uid          person identifier
     * @param pmid         PubMed ID
     * @param entryPath    PM UI entry path (CANDIDATE_LIST or PUBMED_SEARCH)
     * @param epochSeconds curator action time, epoch seconds
     */
    void upsertCuratorAction(String uid, long pmid, EntryPath entryPath, long epochSeconds);
}
