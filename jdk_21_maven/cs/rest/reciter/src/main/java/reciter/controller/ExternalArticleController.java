package reciter.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import reciter.database.dynamodb.model.AnalysisOutput;
import reciter.database.dynamodb.model.ExternalArticle;
import reciter.database.dynamodb.model.FeedbackLog;
import reciter.database.dynamodb.model.GoldStandard;
import reciter.engine.analysis.ReCiterArticleFeature;
import reciter.service.AnalysisService;
import reciter.service.ExternalArticleService;
import reciter.service.FeedbackLogService;
import reciter.service.IdentityService;
import reciter.service.dynamo.ExternalArticleDupCheck;
import reciter.service.dynamo.IDynamoDbGoldStandardService;

/**
 * CRUD for publications manually added from non-PubMed sources (Scopus, Web of
 * Science, OpenAlex, WorldCat, Crossref). These records never enter feature generation or scoring;
 * they surface in feature-generator output only via includeExternal=true.
 */
@Tag(name = "ExternalArticleController", description = "Operations on manually added external-source articles.")
@RestController
public class ExternalArticleController {

    private static final Logger log = LoggerFactory.getLogger(ExternalArticleController.class);

    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile(
            "^(?:(SCOPUS|WOS|OPENALEX|WORLDCAT):\\S+|CROSSREF:10\\.\\d{4,9}/[^\\sA-Z]+)$");

    /** reciterdb external_article.article_id is VARCHAR(96); a CROSSREF id carries the whole DOI. */
    static final int MAX_CROSSREF_ARTICLE_ID_LENGTH = 96;

    private static final String ARTICLE_ID_FORMAT_ERROR = "articleId is required and must be a prefixed identifier: "
            + "SCOPUS:<id>, WOS:<id>, OPENALEX:<id>, WORLDCAT:<id>, or CROSSREF:<lowercased DOI starting 10.>.";

    @Autowired
    private ExternalArticleService externalArticleService;

    @Autowired
    private IDynamoDbGoldStandardService dynamoDbGoldStandardService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private FeedbackLogService feedbackLogService;

    @Operation(summary = "Add an external-source article for a person.",
            description = "Adds a publication from Scopus, Web of Science, OpenAlex, WorldCat, or Crossref. Blocks on PMID/DOI duplicates "
                    + "against the person's PubMed record and existing external articles; warns on fuzzy title+year "
                    + "collisions unless force=true.")
    @PostMapping(value = "/reciter/external-article/by/uid", produces = "application/json")
    public ResponseEntity<Object> addExternalArticle(@RequestParam(value = "uid") String uid,
                                                     @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
                                                     @RequestBody ExternalArticle externalArticle) {
        externalArticle.setUid(uid.trim());
        String validationError = validate(externalArticle);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(validationError));
        }
        if (identityService.findByUid(externalArticle.getUid()) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody("The uid provided '" + externalArticle.getUid() + "' was not found in the Identity table"));
        }

        GoldStandard goldStandard = dynamoDbGoldStandardService.findByUid(externalArticle.getUid());
        ExternalArticleDupCheck.Result result = ExternalArticleDupCheck.check(
                externalArticle,
                goldStandard == null ? null : goldStandard.getKnownPmids(),
                goldStandard == null ? null : goldStandard.getRejectedPmids(),
                candidateArticles(externalArticle.getUid()),
                externalArticleService.findByUid(externalArticle.getUid()));

        if (result.getLevel() == ExternalArticleDupCheck.Level.BLOCKED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictBody("BLOCKED", result,
                    "This article already exists in the person's record."));
        }
        if (result.getLevel() == ExternalArticleDupCheck.Level.WARNING && !force) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictBody("WARNING", result,
                    "Possible duplicate. Retry with force=true to add anyway."));
        }

        if (externalArticle.getDateAdded() == null || externalArticle.getDateAdded().isBlank()) {
            externalArticle.setDateAdded(Instant.now().toString());
        }
        // Owned by the supersede rule (#660), never by the client.
        externalArticle.setSuppressed(Boolean.FALSE);
        externalArticle.setSupersededByPmid(null);
        externalArticleService.save(externalArticle);
        logFeedback(externalArticle.getUid(), externalArticle.getArticleId(),
                FeedbackLogService.Feedback.ACCEPTED, externalArticle.getAddedBy(), null);
        log.info("External article {} added for uid {} from {} (method={}, force={})",
                externalArticle.getArticleId(), externalArticle.getUid(), externalArticle.getSourceType(),
                externalArticle.getMethod(), force);
        return ResponseEntity.status(HttpStatus.CREATED).body(externalArticle);
    }

    @Operation(summary = "List external-source articles for a person.",
            description = "Returns all manually added external articles for the uid, including suppressed rows.")
    @GetMapping(value = "/reciter/external-article/by/uid",  produces = "application/json")
    public ResponseEntity<List<ExternalArticle>> findExternalArticles(@RequestParam(value = "uid") String uid) {
        return new ResponseEntity<>(externalArticleService.findByUid(uid.trim()), HttpStatus.OK);
    }

    @Operation(summary = "Delete an external-source article for a person.",
            description = "Deleting is the revoke path — there is deliberately no sameAs assert/revoke model.")
    @DeleteMapping(value = "/reciter/external-article/by/uid", produces = "application/json")
    public ResponseEntity<Object> deleteExternalArticle(@RequestParam(value = "uid") String uid,
                                                        @RequestParam(value = "articleId") String articleId,
                                                        @RequestParam(value = "actorPersonIdentifier", required = false) String actorPersonIdentifier) {
        ExternalArticle existing = externalArticleService.find(uid.trim(), articleId.trim());
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody("No external article '" + articleId + "' found for uid '" + uid + "'."));
        }
        externalArticleService.delete(uid.trim(), articleId.trim());
        // Delete is the revoke of a prior accept — logged as PENDING (back to no assertion),
        // matching the reopen/undo vocabulary. Actor is optional until PM starts sending it.
        logFeedback(uid.trim(), articleId.trim(), FeedbackLogService.Feedback.PENDING,
                actorPersonIdentifier, null);
        log.info("External article {} deleted for uid {}", articleId, uid);
        return new ResponseEntity<>(existing, HttpStatus.OK);
    }

    @Operation(summary = "Record feedback on an external-source article.",
            description = "One endpoint for curator actions (reject/dismiss/reopen a Scopus/OpenAlex/Crossref candidate) and "
                    + "faculty actions (dispute/retract/resolve an already-accepted row). Appends one FeedbackLog row "
                    + "per call — who acted distinguishes reject-from-dispute, not the action name. REJECTED suppresses "
                    + "an existing ExternalArticle row; ACCEPTED un-suppresses it unless the supersede rule owns the "
                    + "suppression (supersededByPmid set); PENDING only logs. A candidate that was never added has no "
                    + "row to flip — the feedback is still logged. REJECTED also takes ownership of the suppression "
                    + "(clears supersededByPmid) so the supersede reconciler cannot resurrect an explicitly rejected "
                    + "row. actorPersonIdentifier is trusted as-is: the caller (the PM proxy) derives it from the "
                    + "acting user's session, never the browser request — same trust-boundary recipe as the "
                    + "goldstandard endpoint's curatedBy param.")
    @PatchMapping(value = "/reciter/external-article/feedback", produces = "application/json")
    public ResponseEntity<Object> recordExternalArticleFeedback(@RequestBody FeedbackRequest request) {
        if (request == null || request.getUid() == null || request.getUid().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody("uid is required."));
        }
        if (articleIdError(request.getArticleId()) != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody(articleIdError(request.getArticleId())));
        }
        if (request.getActorPersonIdentifier() == null || request.getActorPersonIdentifier().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody("actorPersonIdentifier is required."));
        }
        FeedbackLogService.Feedback feedback;
        try {
            feedback = FeedbackLogService.Feedback.valueOf(
                    request.getAction() == null ? "" : request.getAction().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody("action is required and must be one of ACCEPTED, REJECTED, PENDING."));
        }
        String uid = request.getUid().trim();
        String articleId = request.getArticleId().trim();
        if (identityService.findByUid(uid) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody("The uid provided '" + uid + "' was not found in the Identity table"));
        }

        ExternalArticle existing = externalArticleService.find(uid, articleId);
        // The FeedbackLog row is the authoritative record here (suppressed is only a
        // denormalized cache), so it is written first and its failure fails the request
        // before any state changes — unlike the add/delete paths, where it is a side-log.
        if (!logFeedback(uid, articleId, feedback, request.getActorPersonIdentifier().trim(), request.getNote())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Failed to record the feedback; nothing was changed. Retry."));
        }
        if (existing != null) {
            if (feedback == FeedbackLogService.Feedback.REJECTED) {
                // A human rejection takes ownership of the suppression: clearing
                // supersededByPmid keeps reconcileWithGoldStandard's auto-un-suppress
                // (which fires when the superseding PMID leaves the gold standard)
                // from resurrecting an explicitly rejected row.
                existing.setSuppressed(Boolean.TRUE);
                existing.setSupersededByPmid(null);
                externalArticleService.save(existing);
            } else if (feedback == FeedbackLogService.Feedback.ACCEPTED
                    && existing.getSupersededByPmid() == null) {
                // A supersede-owned suppression (#660) is not feedback's to clear.
                existing.setSuppressed(Boolean.FALSE);
                externalArticleService.save(existing);
            }
        }
        log.info("External article {} for uid {} feedback {} by {}", articleId, uid, feedback,
                request.getActorPersonIdentifier().trim());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "LOGGED");
        body.put("uid", uid);
        body.put("articleId", articleId);
        body.put("action", feedback.name());
        body.put("rowExists", existing != null);
        body.put("suppressed", existing == null ? null : existing.getSuppressed());
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    /** PATCH body for {@link #recordExternalArticleFeedback}. */
    @Data
    public static class FeedbackRequest {
        private String uid;
        private String articleId;
        private String action;
        private String actorPersonIdentifier;
        private String note;
    }

    /**
     * Append to FeedbackLog. recordAction never throws; the boolean says whether the row
     * was written — the add/delete paths ignore it (best-effort side-log, matching the
     * GoldStandard path), the feedback endpoint fails the request on it.
     */
    private boolean logFeedback(String uid, String articleId, FeedbackLogService.Feedback feedback,
                                String actorPersonIdentifier, String note) {
        FeedbackLog logEntry = new FeedbackLog();
        logEntry.setUid(uid);
        logEntry.setArticleId(articleId);
        logEntry.setFeedback(feedback.name());
        logEntry.setCuratedBy(0); // no admin_users.userID on this path; actorPersonIdentifier carries identity
        logEntry.setActorPersonIdentifier(actorPersonIdentifier);
        logEntry.setNote(note);
        long epoch = Instant.now().getEpochSecond();
        logEntry.setCreateTimestamp(epoch);
        logEntry.setModifyTimestamp(epoch);
        return feedbackLogService.recordAction(logEntry);
    }

    private String validate(ExternalArticle externalArticle) {
        String articleIdError = articleIdError(externalArticle.getArticleId());
        if (articleIdError != null) {
            return articleIdError;
        }
        externalArticle.setArticleId(externalArticle.getArticleId().trim());
        if (externalArticle.getTitle() == null || externalArticle.getTitle().isBlank()) {
            return "title is required.";
        }
        String prefix = externalArticle.getArticleId().substring(0, externalArticle.getArticleId().indexOf(':'));
        if (externalArticle.getSourceType() == null || externalArticle.getSourceType().isBlank()) {
            externalArticle.setSourceType(prefix);
        } else if (!prefix.equals(externalArticle.getSourceType().trim())) {
            return "sourceType '" + externalArticle.getSourceType() + "' does not match articleId prefix '" + prefix + "'.";
        }
        if ("CROSSREF".equals(prefix)) {
            // The id is the DOI. Pin the doi field to it so the dup check (which compares doi
            // fields) catches the same work added from OpenAlex/Scopus, in both directions.
            externalArticle.setDoi(externalArticle.getArticleId().substring(prefix.length() + 1));
        }
        return null;
    }

    /** Null when the id is acceptable, else the 400 message. */
    static String articleIdError(String articleId) {
        if (articleId == null || !ARTICLE_ID_PATTERN.matcher(articleId.trim()).matches()) {
            return ARTICLE_ID_FORMAT_ERROR;
        }
        if (articleId.trim().startsWith("CROSSREF:") && articleId.trim().length() > MAX_CROSSREF_ARTICLE_ID_LENGTH) {
            return "articleId '" + articleId.trim() + "' is longer than " + MAX_CROSSREF_ARTICLE_ID_LENGTH
                    + " characters (the reciterdb article_id width); this DOI cannot be added as a CROSSREF article.";
        }
        return null;
    }

    private List<ReCiterArticleFeature> candidateArticles(String uid) {
        try {
            AnalysisOutput analysis = analysisService.findByUid(uid);
            if (analysis != null && analysis.getReCiterFeature() != null
                    && analysis.getReCiterFeature().getReCiterArticleFeatures() != null) {
                return analysis.getReCiterFeature().getReCiterArticleFeatures();
            }
        } catch (Exception e) {
            // Weakened dup check (gold standard PMIDs still block) is better than failing the add.
            log.warn("Could not load Analysis for uid {} during external-article dup check: {}", uid, e.getMessage());
        }
        return new ArrayList<>();
    }

    private Map<String, Object> conflictBody(String status, ExternalArticleDupCheck.Result result, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("message", message);
        body.put("matches", result.getMatches());
        return body;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "INVALID");
        body.put("message", message);
        return body;
    }
}
