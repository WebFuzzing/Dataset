package reciter.service;

import java.util.List;

import reciter.database.dynamodb.model.ExternalArticle;

public interface ExternalArticleService {

    void save(ExternalArticle externalArticle);

    List<ExternalArticle> findByUid(String uid);

    ExternalArticle find(String uid, String articleId);

    void delete(String uid, String articleId);

    /**
     * Supersede rule, run after gold standard updates. Re-reads the authoritative
     * gold standard and reconciles both directions: suppresses active rows whose
     * PMID or DOI matches an accepted PMID (DOIs resolved via the person's
     * Analysis output), and un-suppresses rows whose superseding PMID is no
     * longer accepted. Never throws; returns the number of rows changed.
     */
    int reconcileWithGoldStandard(String uid);
}
