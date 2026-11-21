/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package reciter.engine.erroranalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import reciter.model.article.ReCiterArticle;

/**
 * Computes precision, recall, and accuracy for ReCiter article suggestions.
 *
 * <p><b>PENDING article handling (fixed 2026-02):</b> Articles with
 * {@code goldStandard == 0} (PENDING / no curator decision) are excluded
 * from all confusion-matrix counts. The old implementation treated them as
 * false positives, which artificially deflated precision whenever undecided
 * articles scored above the suggestion threshold.</p>
 *
 * <p><b>Precision formula (fixed 2026-02):</b> Now correctly computed as
 * {@code TP / (TP + FP)}. The old implementation used
 * {@code TP / selectedClusterSize} (total articles), which is not standard
 * precision.</p>
 *
 * <p><b>Accuracy formula (fixed 2026-02):</b> Now computed as
 * {@code (TP + TN) / (TP + TN + FP + FN)}. The old implementation used
 * {@code (precision + recall) / 2}.</p>
 *
 * @author jil3004
 */
public class Analysis {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(Analysis.class);
	
    private double precision;
    private double recall;
    private double accuracy;

    private int truePos;
    private int trueNeg;
    private int falseNeg;
    private int falsePos;
    private int goldStandardSize;
    private int selectedClusterSize;
    private int pendingSkippedCount;
    private List<Long> truePositiveList = new ArrayList<>();
    private List<Long> trueNegativeList = new ArrayList<>();
    private List<Long> falsePositiveList = new ArrayList<>();
    private List<Long> falseNegativeList = new ArrayList<>();

    public Analysis() {
    }

    /**
     * Assign gold standard labels to each ReCiterArticle based on accepted/rejected PMID lists.
     *
     * @param reCiterArticles articles to label
     * @param acceptedPmids   PMIDs with ACCEPTED curator decision (goldStandard = 1)
     * @param rejectedPmids   PMIDs with REJECTED curator decision (goldStandard = -1)
     */
    public static void assignGoldStandard(List<ReCiterArticle> reCiterArticles,
                                          List<Long> acceptedPmids,
                                          List<Long> rejectedPmids) {
        Set<Long> acceptedSet = new HashSet<>();
        if (acceptedPmids != null) {
            acceptedSet.addAll(acceptedPmids);
        }

        for (ReCiterArticle article : reCiterArticles) {
            if (acceptedSet.contains(article.getArticleId())) {
                article.setGoldStandard(1);
            }
        }

        if (rejectedPmids != null) {
            Set<Long> rejectedSet = new HashSet<>(rejectedPmids);
            for (ReCiterArticle article : reCiterArticles) {
                if (rejectedSet.contains(article.getArticleId())) {
                    article.setGoldStandard(-1);
                }
            }
        }
    }

    /**
     * Compute precision/recall/accuracy over a set of articles.
     *
     * <p>Called by {@code ReCiterController} (7 filter branches) and
     * {@code ReCiterFeatureGenerator}.</p>
     *
     * @param articles          articles to evaluate; each must have
     *                          {@code goldStandard} set (1 = ACCEPTED,
     *                          -1 = REJECTED, 0 = PENDING/NULL)
     * @param goldStandardPmids PMIDs of accepted (ground-truth positive)
     *                          articles for this person
     * @return populated Analysis object
     */
    public static Analysis performAnalysis(List<ReCiterArticle> articles,
                                           List<Long> goldStandardPmids) {
        Analysis analysis = new Analysis();

        if (goldStandardPmids == null || goldStandardPmids.isEmpty()) {
            return analysis;
        }

        Set<Long> goldSet = new HashSet<>(goldStandardPmids);
        Set<Long> articlePmids = new HashSet<>();
        int pendingSkipped = 0;

        analysis.setGoldStandardSize(goldStandardPmids.size());
        analysis.setSelectedClusterSize(articles.size());

        for (ReCiterArticle article : articles) {
            long pmid = article.getArticleId();
            articlePmids.add(pmid);

            // --- FIX: skip PENDING articles (goldStandard == 0) ---
            // These have no curator decision and must not inflate FP count.
            if (article.getGoldStandard() == 0) {
                pendingSkipped++;
                continue;
            }

            if (goldSet.contains(pmid)) {
                // Ground-truth positive and present in article set → TP
                analysis.getTruePositiveList().add(pmid);
            } else if (article.getGoldStandard() == -1) {
                // Confirmed rejected and present in article set → FP
                analysis.getFalsePositiveList().add(pmid);
            } else {
                // goldStandard == 1 but not in goldSet — data inconsistency;
            	// treat conservatively as TN, but surface it (FIX #640-D) so gold/cache
                // divergence is no longer silently absorbed.
                log.warn("Accepted article pmid={} is labelled goldStandard==1 but is absent from the supplied gold-standard set (size={}); classifying as TN. Gold standard and cached analysis may be out of sync.", pmid, goldSet.size());
                analysis.getTrueNegativeList().add(pmid);
            }
        }

        // False negatives: accepted articles NOT present in the article set
        for (Long pmid : goldStandardPmids) {
            if (!articlePmids.contains(pmid)) {
                analysis.getFalseNegativeList().add(pmid);
            }
        }

        analysis.setTruePos(analysis.getTruePositiveList().size());
        analysis.setTrueNeg(analysis.getTrueNegativeList().size());
        analysis.setFalsePos(analysis.getFalsePositiveList().size());
        analysis.setFalseNeg(analysis.getFalseNegativeList().size());
        analysis.setPendingSkippedCount(pendingSkipped);

        // Cache computed values
        analysis.setPrecision(analysis.getPrecision());
        analysis.setRecall(analysis.getRecall());
        analysis.setAccuracy(analysis.getAccuracy());

        return analysis;
    }

    // --- Corrected metric formulas ---

    /**
     * Precision = TP / (TP + FP).
     * <p>Old (buggy): TP / selectedClusterSize.</p>
     */
    public double getPrecision() {
        int denominator = truePos + falsePos;
        if (denominator == 0) return 0;
        return (double) truePos / denominator;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    /**
     * Recall = TP / goldStandardSize (= TP / (TP + FN)).
     */
    public double getRecall() {
        if (goldStandardSize == 0) return 0;
        return (double) truePos / goldStandardSize;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    /**
     * Accuracy = (TP + TN) / (TP + TN + FP + FN).
     * <p>Old (buggy): (precision + recall) / 2.</p>
     */
    public double getAccuracy() {
        int total = truePos + trueNeg + falsePos + falseNeg;
        if (total == 0) return 0;
        return (double) (truePos + trueNeg) / total;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    // --- Getters and setters ---

    public int getTruePos() {
        return truePos;
    }

    public void setTruePos(int truePos) {
        this.truePos = truePos;
    }

    public int getTrueNeg() {
        return trueNeg;
    }

    public void setTrueNeg(int trueNeg) {
        this.trueNeg = trueNeg;
    }

    public int getFalsePos() {
        return falsePos;
    }

    public void setFalsePos(int falsePos) {
        this.falsePos = falsePos;
    }

    public int getFalseNeg() {
        return falseNeg;
    }

    public void setFalseNeg(int falseNeg) {
        this.falseNeg = falseNeg;
    }

    public int getGoldStandardSize() {
        return goldStandardSize;
    }

    public void setGoldStandardSize(int goldStandardSize) {
        this.goldStandardSize = goldStandardSize;
    }

    public int getSelectedClusterSize() {
        return selectedClusterSize;
    }

    public void setSelectedClusterSize(int selectedClusterSize) {
        this.selectedClusterSize = selectedClusterSize;
    }

    public int getPendingSkippedCount() {
        return pendingSkippedCount;
    }

    public void setPendingSkippedCount(int pendingSkippedCount) {
        this.pendingSkippedCount = pendingSkippedCount;
    }

    public List<Long> getTruePositiveList() {
        return truePositiveList;
    }

    public void setTruePositiveList(List<Long> truePositiveList) {
        this.truePositiveList = truePositiveList;
    }

    public List<Long> getTrueNegativeList() {
        return trueNegativeList;
    }

    public void setTrueNegativeList(List<Long> trueNegativeList) {
        this.trueNegativeList = trueNegativeList;
    }

    public List<Long> getFalsePositiveList() {
        return falsePositiveList;
    }

    public void setFalsePositiveList(List<Long> falsePositiveList) {
        this.falsePositiveList = falsePositiveList;
    }

    public List<Long> getFalseNegativeList() {
        return falseNegativeList;
    }

    public void setFalseNegativeList(List<Long> falseNegativeList) {
        this.falseNegativeList = falseNegativeList;
    }

    @Override
    public String toString() {
        return "Analysis [precision=" + getPrecision()
                + ", recall=" + getRecall()
                + ", accuracy=" + getAccuracy()
                + ", truePos=" + truePos
                + ", trueNeg=" + trueNeg
                + ", falsePos=" + falsePos
                + ", falseNeg=" + falseNeg
                + ", pendingSkipped=" + pendingSkippedCount
                + ", goldStandardSize=" + goldStandardSize
                + ", selectedClusterSize=" + selectedClusterSize
                + "]";
    }
}
