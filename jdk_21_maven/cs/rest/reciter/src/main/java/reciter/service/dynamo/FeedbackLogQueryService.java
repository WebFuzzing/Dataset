package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.FeedbackLog;
import reciter.database.dynamodb.repository.FeedbackLogQueryRepository;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Read-side service for curation audit history (Phase 34).
 *
 * <p>Queries the {@code FeedbackLog} table (curator accept/reject/pending actions)
 * by {@code uid} and enriches each row with the matching {@code ArticleProvenance}
 * record (how the PMID first arrived). Delegates all DynamoDB access to
 * {@link FeedbackLogQueryRepository}, mirroring the write side
 * ({@link FeedbackLogServiceImpl}, {@link ArticleProvenanceServiceImpl}).
 */
@Service
public class FeedbackLogQueryService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackLogQueryService.class);
    private static final int MAX_ROWS = 2000;

    private final FeedbackLogQueryRepository feedbackLogQueryRepository;

    public FeedbackLogQueryService(FeedbackLogQueryRepository feedbackLogQueryRepository) {
        this.feedbackLogQueryRepository = feedbackLogQueryRepository;
    }

    /**
     * Return the curation audit history for a uid, newest action first, each row
     * enriched with the article's retrieval provenance. Capped at {@value #MAX_ROWS}
     * rows (logged if truncated).
     */
    public List<AuditHistoryEntry> getAuditHistory(String uid) {
        List<AuditHistoryEntry> entries = new ArrayList<>();
        if (uid == null || uid.isBlank()) {
            return entries;
        }

        Map<String, ProvenanceInfo> provenance = loadProvenance(uid);

        List<FeedbackLog> feedbackLogs;
        try {
            feedbackLogs = feedbackLogQueryRepository.findFeedbackLogsByUid(uid);
        } catch (Exception e) {
            log.warn("Failed to load FeedbackLog for uid={}: {}", uid, e.getMessage(), e);
            return entries;
        }

        for (FeedbackLog feedbackLog : feedbackLogs) {
            entries.add(toEntry(feedbackLog, provenance));
            if (entries.size() >= MAX_ROWS) {
                log.warn("FeedbackLog audit history for uid={} truncated at {} rows", uid, MAX_ROWS);
                return entries;
            }
        }

        return entries;
    }

    private AuditHistoryEntry toEntry(FeedbackLog feedbackLog, Map<String, ProvenanceInfo> provenance) {
        AuditHistoryEntry e = new AuditHistoryEntry();
        e.articleId       = feedbackLog.getArticleId();
        e.feedback        = feedbackLog.getFeedback();
        e.curatedBy       = feedbackLog.getCuratedBy();
        e.actorPersonIdentifier = feedbackLog.getActorPersonIdentifier();
        e.note            = feedbackLog.getNote();
        e.createTimestamp = feedbackLog.getCreateTimestamp();
        e.sk              = feedbackLog.getSk();
        e.src             = feedbackLog.getSrc();
        ProvenanceInfo p  = (e.articleId != null) ? provenance.get(e.articleId) : null;
        if (p != null) {
            e.provenanceRs  = p.rs;
            e.provenanceFrd = p.frd;
            e.provenanceSrc = p.src;
        }
        return e;
    }

    /** Load all ArticleProvenance rows for a uid into an articleId -> provenance map. */
    private Map<String, ProvenanceInfo> loadProvenance(String uid) {
        Map<String, ProvenanceInfo> map = new HashMap<>();
        try {
            List<Map<String, AttributeValue>> provenanceItems =
                    feedbackLogQueryRepository.findProvenanceByUid(uid);

            for (Map<String, AttributeValue> item : provenanceItems) {
                String articleId = getS(item, "articleId");
                if (articleId == null) {
                    continue;
                }
                ProvenanceInfo p = new ProvenanceInfo();
                p.rs  = getS(item, "rs");
                p.frd = item.containsKey("frd") ? getNLong(item, "frd") : null;
                p.src = getS(item, "src");
                map.put(articleId, p);
            }
        } catch (Exception e) {
            // Provenance is enrichment only; never fail the audit-history read on it.
            log.warn("Failed to load ArticleProvenance for uid={}: {}", uid, e.getMessage(), e);
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // Attribute extraction helpers (used for raw ArticleProvenance map only)
    // -------------------------------------------------------------------------

    private static String getS(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        return (v == null) ? null : v.s();
    }

    private static long getNLong(Map<String, AttributeValue> item, String key) {
        AttributeValue v = item.get(key);
        if (v == null || v.n() == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v.n());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static class ProvenanceInfo {
        String rs;
        Long   frd;
        String src;
    }
}
