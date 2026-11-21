package reciter.service.dynamo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.FeedbackLog;
import reciter.database.dynamodb.repository.FeedbackLogRepository;
import reciter.service.FeedbackLogService;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/**
 * Phase 33-02 implementation of {@link FeedbackLogService}.
 *
 * <p>Uses raw {@code AmazonDynamoDB.putItem} with {@code attribute_not_exists(sk)}
 * for idempotency. The sk is {@code "<epoch_seconds>#<nano-derived-hex>"} which
 * gives microsecond-resolution uniqueness within a single process; cross-process
 * collisions are caught by the conditional and retried with a fresh suffix.
 *
 * <p>{@code src='MAN'} on every row — Phase 32 D-03: PM UI is the only source for
 * this table going forward (CTSC ingestion bypasses PM and writes directly to
 * GoldStandard).
 */
@Service
public class FeedbackLogServiceImpl implements FeedbackLogService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackLogServiceImpl.class);
    private static final String SRC_MAN = "MAN";
    private static final int SK_RETRY_LIMIT = 5;

    private final FeedbackLogRepository feedbackLogRepository;

    public FeedbackLogServiceImpl(FeedbackLogRepository feedbackLogRepository) {
        this.feedbackLogRepository = feedbackLogRepository;
    }

    @Override
	public boolean recordAction(FeedbackLog feedbackLog) {
		if (feedbackLog == null || feedbackLog.getUid() == null || feedbackLog.getUid().isBlank()) {
			log.warn("recordAction called with null/empty uid; skipping (pmid={})",
					feedbackLog == null ? null : feedbackLog.getArticleId());
			return false;
		}
		if (feedbackLog.getFeedback() == null) {
			log.warn("recordAction called with null feedback; skipping (uid={} pmid={})", feedbackLog.getUid(),
					feedbackLog.getArticleId());
			return false;
		}

		for (int attempt = 0; attempt < SK_RETRY_LIMIT; attempt++) {
			String sk = buildSk(feedbackLog.getCreateTimestamp());
			feedbackLog.setSk(sk);
			feedbackLog.setSrc(SRC_MAN);

			try {
				// Pass the object directly just like ESearchResultRepository!
				feedbackLogRepository.save(feedbackLog);
				return true;
			} catch (ConditionalCheckFailedException race) {
				// If a collision happens, log it and the loop will retry with a fresh SK
				log.info("FeedbackLog sk collision for uid={} sk={}; retrying (attempt {})", feedbackLog.getUid(), sk,
						attempt + 1);
			} catch (DynamoDbException e) {
				log.warn("FeedbackLog write failed for uid={} articleId={}: {}", feedbackLog.getUid(),
						feedbackLog.getArticleId(), e.getMessage());
				return false;
			} catch (RuntimeException e) {
				// SdkClientException (network/credentials) is not a DynamoDbException —
				// without this, a logging failure would surface as a 500 after the
				// caller's durable write already committed.
				log.warn("FeedbackLog write failed for uid={} articleId={}: {}", feedbackLog.getUid(),
						feedbackLog.getArticleId(), e.toString());
				return false;
			}
		}
		log.warn("FeedbackLog write gave up after {} sk collisions for uid={} articleId={}", SK_RETRY_LIMIT,
				feedbackLog.getUid(), feedbackLog.getArticleId());
		return false;
	}

    /**
     * Build a unique sk: epoch + '#' + last 8 hex digits of nanoTime.
     * Within a single JVM, nanoTime is monotonically increasing, so collisions
     * within a process are vanishingly rare. Cross-process collisions are caught
     * by the conditional put.
     */
    private String buildSk(long epochSeconds) {
        long nano = System.nanoTime();
        // Mask to 32 bits (8 hex digits)
        String hexSuffix = String.format("%08x", (int) (nano & 0xFFFFFFFFL));
        return epochSeconds + "#" + hexSuffix;
    }
}
