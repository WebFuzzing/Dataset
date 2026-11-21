package reciter.engine.erroranalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import reciter.model.article.ReCiterArticle;

/**
 * Regression coverage for the precision/recall/accuracy formulas in
 * {@link Analysis#performAnalysis(List, List)}, which were corrected in 2026-02
 * (PENDING articles excluded from the confusion matrix; precision = TP/(TP+FP);
 * accuracy = (TP+TN)/total). Pure function — no mocks, no AWS.
 */
public class AnalysisTest {

    private static final int ACCEPTED = 1;
    private static final int REJECTED = -1;
    private static final int PENDING = 0;
    private static final double DELTA = 1e-9;

    private static ReCiterArticle article(long pmid, int goldStandard) {
        ReCiterArticle a = new ReCiterArticle(pmid);
        a.setGoldStandard(goldStandard);
        return a;
    }

    @Test
    public void allAcceptedPresent_precisionAndRecallAreOne() {
        Analysis a = Analysis.performAnalysis(
                Arrays.asList(article(1L, ACCEPTED), article(2L, ACCEPTED)),
                Arrays.asList(1L, 2L));

        assertEquals(2, a.getTruePos());
        assertEquals(0, a.getFalsePos());
        assertEquals(0, a.getFalseNeg());
        assertEquals(1.0, a.getPrecision(), DELTA);
        assertEquals(1.0, a.getRecall(), DELTA);
        assertEquals(1.0, a.getAccuracy(), DELTA);
    }

    @Test
    public void acceptedMissingFromCluster_lowersRecall() {
        // pmid 2 is accepted in the gold standard but absent from the article set -> FN
        Analysis a = Analysis.performAnalysis(
                Arrays.asList(article(1L, ACCEPTED)),
                Arrays.asList(1L, 2L));

        assertEquals(1, a.getTruePos());
        assertEquals(1, a.getFalseNeg());
        assertEquals(1.0, a.getPrecision(), DELTA); // TP/(TP+FP) = 1/1
        assertEquals(0.5, a.getRecall(), DELTA);    // TP/goldSize = 1/2
    }

    @Test
    public void confirmedRejectedPresent_lowersPrecision() {
        // pmid 3 is a confirmed rejection present in the cluster -> FP
        Analysis a = Analysis.performAnalysis(
                Arrays.asList(article(1L, ACCEPTED), article(3L, REJECTED)),
                Arrays.asList(1L));

        assertEquals(1, a.getTruePos());
        assertEquals(1, a.getFalsePos());
        assertEquals(0.5, a.getPrecision(), DELTA); // 1/(1+1)
        assertEquals(1.0, a.getRecall(), DELTA);    // 1/1
    }

    @Test
    public void pendingArticleIsNotCountedAsFalsePositive() {
        // The core 2026-02 fix: a PENDING (goldStandard==0) article above threshold
        // must be skipped, not counted as FP.
        Analysis a = Analysis.performAnalysis(
                Arrays.asList(article(1L, ACCEPTED), article(5L, PENDING)),
                Arrays.asList(1L));

        assertEquals(1, a.getTruePos());
        assertEquals(0, a.getFalsePos());
        assertEquals(1, a.getPendingSkippedCount());
        assertEquals(1.0, a.getPrecision(), DELTA);
    }

    @Test
    public void nullOrEmptyGoldStandard_returnsZeroedAnalysis() {
        List<ReCiterArticle> articles = Arrays.asList(article(1L, ACCEPTED));

        Analysis withNull = Analysis.performAnalysis(articles, null);
        assertEquals(0, withNull.getTruePos());
        assertEquals(0.0, withNull.getPrecision(), DELTA);
        assertEquals(0.0, withNull.getRecall(), DELTA);
        assertEquals(0.0, withNull.getAccuracy(), DELTA);

        Analysis withEmpty = Analysis.performAnalysis(articles, Collections.emptyList());
        assertEquals(0.0, withEmpty.getPrecision(), DELTA);
        assertEquals(0.0, withEmpty.getRecall(), DELTA);
    }

    @Test
    public void noTruePositivesOrFalsePositives_metricsAreZeroNotNaN() {
        // Empty cluster, non-empty gold standard -> only FNs. Guards must avoid
        // 0/0 = NaN in precision/accuracy.
        Analysis a = Analysis.performAnalysis(
                Collections.emptyList(),
                Arrays.asList(1L));

        assertEquals(0, a.getTruePos());
        assertEquals(0, a.getFalsePos());
        assertEquals(1, a.getFalseNeg());
        assertEquals(0.0, a.getPrecision(), DELTA);
        assertEquals(0.0, a.getRecall(), DELTA);
        assertEquals(0.0, a.getAccuracy(), DELTA);
        assertFalse("precision must not be NaN", Double.isNaN(a.getPrecision()));
        assertFalse("accuracy must not be NaN", Double.isNaN(a.getAccuracy()));
    }

    @Test
    public void acceptedLabelNotInGoldSet_treatedAsTrueNegative() {
        // Data-inconsistency branch: article labelled accepted (goldStandard==1) but
        // its pmid is not in the supplied gold-standard list -> conservatively TN.
        Analysis a = Analysis.performAnalysis(
                Arrays.asList(article(7L, ACCEPTED)),
                Arrays.asList(8L));

        assertEquals(0, a.getTruePos());
        assertEquals(1, a.getTrueNeg());
        assertEquals(0, a.getFalsePos());
        assertEquals(1, a.getFalseNeg()); // pmid 8 missing from cluster
    }

    @Test
    public void assignGoldStandard_labelsAcceptedAndRejectedLeavingOthersPending() {
        List<ReCiterArticle> articles = new ArrayList<>(Arrays.asList(
                article(1L, PENDING), article(2L, PENDING), article(3L, PENDING)));

        Analysis.assignGoldStandard(articles, Arrays.asList(1L), Arrays.asList(2L));

        assertEquals(ACCEPTED, articles.get(0).getGoldStandard());
        assertEquals(REJECTED, articles.get(1).getGoldStandard());
        assertEquals(PENDING, articles.get(2).getGoldStandard());
    }
}
