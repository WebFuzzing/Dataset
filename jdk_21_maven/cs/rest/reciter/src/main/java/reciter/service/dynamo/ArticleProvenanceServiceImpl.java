package reciter.service.dynamo;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reciter.database.dynamodb.model.ArticleProvenance;
import reciter.database.dynamodb.repository.ArticleProvenanceRepository;
import reciter.feedback.EntryPath;
import reciter.service.ArticleProvenanceService;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/**
 * Phase 33-01 implementation of {@link ArticleProvenanceService}.
 *
 * <p>Uses raw {@code AmazonDynamoDB.updateItem} so the {@code UpdateExpression} can
 * combine {@code if_not_exists(...)} with {@code ADD ads :strategySet} in a single
 * 
 * does not support this expression shape directly.
 *
 * <p>Single-round-trip semantics: no read-then-write, no race; concurrent retrieval
 * strategies on the same PMID are safe because String Set ADD is commutative and
 * {@code if_not_exists} is atomic per UpdateItem.
 */
@Service
@RequiredArgsConstructor
public class ArticleProvenanceServiceImpl implements ArticleProvenanceService {

    private static final Logger log = LoggerFactory.getLogger(ArticleProvenanceServiceImpl.class);
    private static final String PM_UI_SEARCH_RS = "PM_UI_SEARCH";
    private static final String SRC_PM = "PM";
    private static final String SRC_CTSC = "CTSC";
    private static final String SRC_GS_PLACEHOLDER = "GS";
    private static final String SRC_MAN = "MAN";
    private static final String SRC_MAN_FROM_PM = "MAN_FROM_PM";
    private static final String SRC_MAN_FROM_CTSC = "MAN_FROM_CTSC";
    private static final String PM_AUTHOR_RS = "PM_AUTHOR";

    private final ArticleProvenanceRepository articleProvenanceRepository;

    @Override
    public void upsertRetrievalProvenance(String uid, long pmid, String strategyCode, long epochSeconds) {
        if (uid == null || uid.isBlank()) {
            log.warn("upsertRetrievalProvenance called with null/empty uid; skipping (pmid={})", pmid);
            return;
        }
        if (strategyCode == null || strategyCode.isBlank()) {
            log.warn("upsertRetrievalProvenance called with null/empty strategyCode; skipping (uid={} pmid={})", uid, pmid);
            return;
        }

        try {
            articleProvenanceRepository.upsertRetrievalProvenance(uid, String.valueOf(pmid), strategyCode, SRC_PM, epochSeconds);
        } catch (DynamoDbException e) {
            log.error("ArticleProvenance upsert failed for uid={} pmid={} strategy={}: {}", uid, pmid, strategyCode, e.getMessage());
        } catch (RuntimeException e) {
            log.error("ArticleProvenance upsert unexpected error for uid={} pmid={} strategy={}: {}", uid, pmid, strategyCode, e.getMessage());
        }
    }

    @Override
    public void upsertCuratorAction(String uid, long pmid, EntryPath entryPath, long epochSeconds) {
        if (uid == null || uid.isBlank()) {
            log.warn("upsertCuratorAction called with null/empty uid; skipping (pmid={})", pmid);
            return;
        }
        EntryPath path = (entryPath == null) ? EntryPath.CANDIDATE_LIST : entryPath;

        if (path == EntryPath.PUBMED_SEARCH) {
            writePmUiSearchRecord(uid, pmid, epochSeconds);
        } else if (path == EntryPath.PM_AUTHOR) {
            // Authorship Review tab: stamp the PM_AUTHOR entry-path marker into rs/ads so
            // these curations are auditable, but do NOT seed src='PM' (see writePmAuthorRecord).
            writePmAuthorRecord(uid, pmid, epochSeconds);
        }

        try {
            applyD11Transition(uid, pmid, epochSeconds, /*allowRetry=*/ true);
        } catch (RuntimeException e) {
            log.error("D-11 upsert unexpected error for uid={} pmid={} entryPath={}: {}", uid, pmid, path, e.getMessage());
        }
    }

    private void writePmUiSearchRecord(String uid, long pmid, long epochSeconds) {
        try {
            articleProvenanceRepository.writePmUiSearchRecord(uid, String.valueOf(pmid), PM_UI_SEARCH_RS, SRC_PM, epochSeconds);
        } catch (DynamoDbException e) {
            log.error("PM_UI_SEARCH retrieval-record write failed for uid={} pmid={}: {}", uid, pmid, e.getMessage());
        }
    }

    private void applyD11Transition(String uid, long pmid, long epochSeconds, boolean allowRetry) {
        String articleId = String.valueOf(pmid);
        Optional<ArticleProvenance> recordOpt;
        
        try {
            recordOpt = articleProvenanceRepository.findByIdWithConsistentRead(uid, articleId);
        } catch (DynamoDbException e) {
            log.error("D-11 read failed for uid={} pmid={}: {}", uid, pmid, e.getMessage());
            return;
        }

        String existingSrc = recordOpt.map(ArticleProvenance::getSrc).orElse(null);
        String newSrc = computeNewSrc(existingSrc);

        try {
            articleProvenanceRepository.applyD11Update(uid, articleId, newSrc, existingSrc, epochSeconds);
        } catch (ConditionalCheckFailedException race) {
            if (allowRetry) {
                log.info("D-11 race for uid={} pmid={} (existing src changed since GetItem); retrying once", uid, pmid);
                applyD11Transition(uid, pmid, epochSeconds, /*allowRetry=*/ false);
            } else {
                log.warn("D-11 retry also failed for uid={} pmid={}; giving up (PM UI request continues)", uid, pmid);
            }
        } catch (DynamoDbException e) {
            log.error("D-11 update failed for uid={} pmid={}: {}", uid, pmid, e.getMessage());
        }
    }
    
    /**
     * Authorship Review tab (PM_AUTHOR) marker write. Mirrors {@link #writePmUiSearchRecord}
     * — sets {@code rs} if absent, ensures {@code frd}, and ADDs PM_AUTHOR to the {@code ads}
     * strategy set — but deliberately does NOT seed {@code src='PM'}.
     */
    private void writePmAuthorRecord(String uid, long pmid, long epochSeconds) {
        try {
            articleProvenanceRepository.writePmAuthorRecord(uid, String.valueOf(pmid), PM_AUTHOR_RS, epochSeconds);
        } catch (DynamoDbException e) {
            log.warn("PM_AUTHOR marker write failed for uid={} pmid={}: {}", uid, pmid, e.getMessage(), e);
        }
    }
   
    static String computeNewSrc(String existingSrc) {
        if (existingSrc == null || SRC_GS_PLACEHOLDER.equals(existingSrc)) {
            return SRC_MAN;
        }
        if (SRC_PM.equals(existingSrc)) {
            return SRC_MAN_FROM_PM;
        }
        if (SRC_CTSC.equals(existingSrc)) {
            return SRC_MAN_FROM_CTSC;
        }
        return existingSrc;
    }
}