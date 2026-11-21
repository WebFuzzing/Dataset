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
package reciter.algorithm.evidence.targetauthor.degree.strategy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.evidence.article.AbstractRemoveReCiterArticleStrategy;
import reciter.engine.EngineParameters;
import reciter.engine.analysis.evidence.EducationYearEvidence;
import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

/**
 * Year-based matching for TargetAuthor (Phase II)
 * 
 * <p> Returns a year discrepancy score between an article's journal's
 * issue pub date year and a target author's terminal degree.
 * 
 * <p>Because this class extends {@code AbstractTargetAuthorStrategy}, it
 * implements the year-based matching for a target author (i.e., Phase II)
 * matching).
 * 
 * @author jil3004
 * @author ved4006	
 *
 */
public class YearDiscrepancyStrategy extends AbstractRemoveReCiterArticleStrategy {

	private static final Logger log = LoggerFactory.getLogger(YearDiscrepancyStrategy.class);
	private final DegreeType degreeType;
	
	

	/**
	 * Constructor for YearDiscrepancyStrategy. Requires a DegreeType.
	 * 
	 * @param degreeType DegreeType used by this strategy.
	 */
	public YearDiscrepancyStrategy(DegreeType degreeType) {
		this.degreeType = degreeType;
	}
	
	public YearDiscrepancyStrategy() {
		this.degreeType = null;
	}

	/**
	 * <p>
	 * Identify year of publications for the article.
	 * Get the target author's terminal degree from rc_identity_degree.

	 * If discrepancy between pub and doctoral degree < -5, mark as false 
	 * </p>
	 * 
	 * <p>
	 * Example: 
	 * pubyear = 1990, doctoral degree = 1994, difference is -4, -5 < -4, therefore do nothing
	 * </p>
	 * 
	 * <p>
	 * Example:
	 * pubyear = 1998, doctoral degree = 1994, difference is 4, -5 < 4, therefore do nothing.
	 * </p>
	 * 
	 * <p>
	 * If discrepancy between pub and bachelor degree < 1, mark as false
	 * Example:
	 * pubyear = 1998, bachelor degree = 1998, difference is 0, 1 < 0 is not true, therefore mark as false
	 * </p>
	 */
	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		if (reCiterArticle != null 												&&
				reCiterArticle.getJournal() != null 							&& 
				reCiterArticle.getJournal().getJournalIssuePubDateYear() != 0) {

			int year = reCiterArticle.getJournal().getJournalIssuePubDateYear();

			int difference;
			if (degreeType.equals(DegreeType.BACHELORS)) {
				if (identity.getDegreeYear().getBachelorYear() != 0) {
					difference = year - identity.getDegreeYear().getBachelorYear();
					reCiterArticle.setBachelorsYearDiscrepancy(difference);
					if (difference < 1) {
						log.info("Bachelors: Identity degree and reCiter article {} journal issue publication date difference < 1. Remove from cluster.", reCiterArticle.getArticleId());
						reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() 
								+ " [Bachelors Degree Difference=" + difference + "]");
						reCiterArticle.setBachelorsYearDiscrepancyScore(1);
						reCiterArticle.setPublishedPriorAcademicDegreeBachelors("Target Author bachelors graduation year: " +
								identity.getDegreeYear().getBachelorYear() + " publication date: " + year + ". Diff="+ difference);
						return 1;
					}
				}
			} else if (degreeType.equals(DegreeType.DOCTORAL)) {
				if (identity.getDegreeYear().getDoctoralYear() != 0) {
					int doctoral = identity.getDegreeYear().getDoctoralYear();
					difference = year - doctoral;
					reCiterArticle.setDoctoralYearDiscrepancy(difference);
					if (doctoral < 1998) {
						if (difference < -6) {
							log.info("DOCTORAL 1998: Identity degree and reCiter article {} journal issue publication date difference < -6" +
									". Remove from cluster.", reCiterArticle.getArticleId());
							reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() 
									+ " [Doctoral Degree Difference (<1988) =" + difference + "]");
							reCiterArticle.setDoctoralYearDiscrepancyScore(1);
							reCiterArticle.setPublishedPriorAcademicDegreeDoctoral("(Case doctoral < 1998) Target Author doctoral graduation year: " +
									identity.getDegreeYear().getDoctoralYear() + " publication date: " + year + ". Diff="+ difference);
							return 1;
						}
					} else {
						if (difference < -13) {
							log.info("DOCTORAL: Identity degree and reCiter article {} journal issue publication date difference < -13. " +
									"Remove from cluster.", reCiterArticle.getArticleId());

							reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() 
									+ " [Doctoral Degree Difference (>=1998) =" + difference + "]");
							reCiterArticle.setDoctoralYearDiscrepancyScore(1);
							reCiterArticle.setPublishedPriorAcademicDegreeDoctoral("Target Author doctoral graduation year: " +
									identity.getDegreeYear().getDoctoralYear() + " publication date: " + year + ". Diff="+ difference);
							return 1;
						}
					}
				}
			}
		}
		return 0;
	}


	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			if (reCiterArticle != null && reCiterArticle.getPublicationDateStandardized() != null) {
				LocalDate date = LocalDate.parse(reCiterArticle.getPublicationDateStandardized());
				int articleYear = date.getYear();

				boolean hasValidDoctoral = identity.getDegreeYear() != null && identity.getDegreeYear().getDoctoralYear() != 0;
				boolean hasValidBachelor = identity.getDegreeYear() != null && identity.getDegreeYear().getBachelorYear() != 0;
				if (hasValidDoctoral || hasValidBachelor) {
					EducationYearEvidence educationYearEvidence = new EducationYearEvidence();
						                  educationYearEvidence.setArticleYear(articleYear);
					double degreeYearDiscrepancyDoctoralScore = 0.0;
					int discrepancyDegreeYearDoctoral = 0;
					double degreeYearDiscrepancyBachelorScore = 0.0;
					int discrepancyDegreeYearBachelor = 0;

					if (hasValidDoctoral) {
						discrepancyDegreeYearDoctoral = articleYear - identity.getDegreeYear().getDoctoralYear();
						discrepancyDegreeYearDoctoral = (discrepancyDegreeYearDoctoral < -99) ? -99
								: discrepancyDegreeYearDoctoral;
						discrepancyDegreeYearDoctoral = (discrepancyDegreeYearDoctoral > 100) ? 100
								: discrepancyDegreeYearDoctoral;
						degreeYearDiscrepancyDoctoralScore = Optional.ofNullable(discrepancyDegreeYearDoctoral)
							    .map(d -> EngineParameters.getDegreeYearDiscrepancyScoreMap().get(Double.valueOf(d)))
							    .orElse(0.0);

						educationYearEvidence.setIdentityDoctoralYear(identity.getDegreeYear().getDoctoralYear());
						educationYearEvidence.setDiscrepancyDegreeYearDoctoral(discrepancyDegreeYearDoctoral);
						educationYearEvidence.setDiscrepancyDegreeYearDoctoralScore(degreeYearDiscrepancyDoctoralScore);
						if (identity.getDegreeYear().getBachelorYear() != 0) {
							educationYearEvidence.setIdentityBachelorYear(identity.getDegreeYear().getBachelorYear());
						}
						reCiterArticle.setEducationYearEvidence(educationYearEvidence);
					} else if (hasValidBachelor) {
						discrepancyDegreeYearBachelor = articleYear - identity.getDegreeYear().getBachelorYear()
								+ ReCiterArticleScorer.strategyParameters.getBachelorYearWeight();
						discrepancyDegreeYearBachelor = (discrepancyDegreeYearBachelor < -99) ? -99
								: discrepancyDegreeYearBachelor;
						discrepancyDegreeYearBachelor = (discrepancyDegreeYearBachelor > 100) ? 100
								: discrepancyDegreeYearBachelor;
						degreeYearDiscrepancyBachelorScore = EngineParameters.getDegreeYearDiscrepancyScoreMap()
								.get(Double.valueOf(discrepancyDegreeYearBachelor));

						educationYearEvidence.setIdentityBachelorYear(identity.getDegreeYear().getBachelorYear());
						educationYearEvidence.setDiscrepancyDegreeYearBachelor(discrepancyDegreeYearBachelor);
						educationYearEvidence.setDiscrepancyDegreeYearBachelorScore(degreeYearDiscrepancyBachelorScore);

						if (identity.getDegreeYear().getDoctoralYear() != 0) {
							educationYearEvidence.setIdentityDoctoralYear(identity.getDegreeYear().getDoctoralYear());
						}
						reCiterArticle.setEducationYearEvidence(educationYearEvidence);
					}

				}
			}
		}
		return 0;
	}
	public DegreeType getDegreeType() {
		return degreeType;
	} 
}

