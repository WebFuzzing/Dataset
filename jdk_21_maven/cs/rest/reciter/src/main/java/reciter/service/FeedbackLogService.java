package reciter.service;

import reciter.database.dynamodb.model.FeedbackLog;

/**
 * Writes curator action records to the {@code FeedbackLog} DynamoDB table.
 *
 * <p>Schema (Phase 32 D-01, D-02):
 * <ul>
 *   <li>{@code uid} (S, PK)</li>
 *   <li>{@code sk} (S, SK) = {@code "<epoch_seconds>#<8-hex-digit-suffix>"}</li>
 *   <li>{@code articleId} (S)</li>
 *   <li>{@code feedback} (S) ∈ {ACCEPTED, REJECTED, PENDING}</li>
 *   <li>{@code curatedBy} (N) — userID from PM</li>
 *   <li>{@code src} (S) = "MAN" (PM UI is the only source for this table going forward)</li>
 *   <li>{@code createTimestamp} (N), {@code modifyTimestamp} (N)</li>
 * </ul>
 *
 * <p>The 8-hex-digit suffix is derived from {@code System.nanoTime()} and gives
 * uniqueness within a single Java process; the conditional {@code attribute_not_exists(sk)}
 * catches cross-process collisions and the caller retries with a fresh suffix.
 */
public interface FeedbackLogService {

    /**
     * Curator action feedback values. See Phase 32 D-04.
     */
    enum Feedback {
        ACCEPTED, REJECTED, PENDING
    }

    /**
     * Record a single curator action. Never throws — all failures are logged and
     * reported through the return value, so callers on best-effort paths can ignore
     * it and callers for whom this row is the authoritative record can react.
     *
     * @param feedbackLog  row to write; uid, feedback, articleId, curatedBy and the
     *                     timestamps are the caller's to set (sk and src are set here)
     * @return true if the row was written, false if it was skipped or the write failed
     */
    boolean recordAction(FeedbackLog feedbackLog);
}
