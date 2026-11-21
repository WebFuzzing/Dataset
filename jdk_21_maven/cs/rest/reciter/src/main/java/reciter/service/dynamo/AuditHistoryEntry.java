package reciter.service.dynamo;


/**
 * One row of curation audit history for the Publication Manager UI: a FeedbackLog
 * action (accept/reject/pending) on a PMID, enriched with that article's
 * ArticleProvenance (how the PMID first arrived). Serialized directly to JSON.
 */
public class AuditHistoryEntry {

    /** PubMed ID the action applied to. */
    public String articleId;
    /** ACCEPTED | REJECTED | PENDING. */
    public String feedback;
    /** Curator's admin_users.userID (0 = unknown; pre-Phase-34 rows are 0). */
    public int curatedBy;
    /** CWID of the person who acted (curator or faculty; null on rows written before the field existed). */
    public String actorPersonIdentifier;
    /** Optional free-text note attached to the action (e.g. a dispute reason). */
    public String note;
    /** Action time, epoch seconds (UTC). */
    public long createTimestamp;
    /** FeedbackLog sort key ({@code <epoch>#<hex>}); useful for stable ordering. */
    public String sk;
    /** Feedback source (always "MAN" for PM-sourced actions). */
    public String src;

    /** ArticleProvenance: first retrieval strategy that found this PMID (how it arrived). */
    public String provenanceRs;
    /** ArticleProvenance: first retrieval date, epoch seconds (nullable). */
    public Long provenanceFrd;
    /** ArticleProvenance: provenance source (PM, MAN, CTSC, MAN_FROM_PM, ...). */
    public String provenanceSrc;
}
