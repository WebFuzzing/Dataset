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
package reciter.algorithm.evidence.targetauthor.affiliation.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.evidence.cluster.averageclustering.strategy.AverageClusteringStrategy;
import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.EngineParameters;
import reciter.engine.Feature;
import reciter.engine.analysis.evidence.AffiliationEvidence;
import reciter.engine.analysis.evidence.NonTargetAuthorScopusAffiliation;
import reciter.engine.analysis.evidence.AffiliationEvidence.InstitutionalAffiliationMatchType;
import reciter.engine.analysis.evidence.AffiliationEvidence.InstitutionalAffiliationSource;
import reciter.engine.analysis.evidence.TargetAuthorPubmedAffiliation;
import reciter.engine.analysis.evidence.TargetAuthorScopusAffiliation;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;
import reciter.model.scopus.Affiliation;
import reciter.model.scopus.Author;

public class CommonAffiliationStrategy extends AbstractTargetAuthorStrategy {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(CommonAffiliationStrategy.class);
	
	private final String[] homeInstScopusInstitutionsIDs = ReCiterArticleScorer.strategyParameters.getInstAfflHomeInstScopusInstIDs().trim().split("\\s*,\\s*");
	private final String[] collaboratingInstScopusInstitutionsIDs = ReCiterArticleScorer.strategyParameters.getInstAfflCollaboratingInstScopusInstIDs().trim().split("\\s*,\\s*");
	private final String[] homeInstitutionsKeywords = ReCiterArticleScorer.strategyParameters.getInstAfflHomeInstKeywords().trim().split("\\s*,\\s*");
	private final String[] collaboratingInstitutionsKeywords = ReCiterArticleScorer.strategyParameters.getInstAfflCollaboratingInstKeywords().trim().split("\\s*,\\s*");
	
	private Set<String> knownAffiliationIds = new HashSet<String>();
	private List<Integer> nonTargetAuthorScopusAffiliationIds = new ArrayList<Integer>();
	private double totalAffiliationScore = 0;
	
	
	public CommonAffiliationStrategy() {
	}
	
	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		double score = 0;
		/*if (containsWeillCornell(reCiterArticle)) {
			reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + "[contains weill cornell and its variant:" + variantName + "]");
			AffiliationEvidence affiliationEvidence = new AffiliationEvidence();
			affiliationEvidence.setInstitutionalAffiliations(identity.getInstitutions());
			affiliationEvidence.setEmails(identity.getEmails());
			affiliationEvidence.setDepartments(identity.getOrganizationalUnits());
			affiliationEvidence.setArticleAffiliation(variantName);
			reCiterArticle.setAffiliationEvidence(affiliationEvidence);
			score = 1;
		}
		reCiterArticle.setAffiliationScore(score);*/
		return score;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		double sum = 0;
		populateKnownAffiliationIds(identity);
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			AffiliationEvidence affiliationEvidence = new AffiliationEvidence();
			for(ReCiterAuthor reCiterAuthor: reCiterArticle.getArticleCoAuthors().getAuthors()) {
				if(reCiterAuthor.isTargetAuthor()) {
					if(ReCiterArticleScorer.strategyParameters.isUseScopusArticles() && ReCiterArticleScorer.strategyParameters.isScopusCommonAffiliation()) {
						if(reCiterArticle.getScopusArticle() != null) {
							//Get the corresponding Scopus Author for the target author
							Author scopusAuthor = reCiterArticle.getScopusArticle().getAuthors().stream().filter(author -> reCiterAuthor.getRank() == author.getSeq()).findFirst().orElse(null);
							List<TargetAuthorScopusAffiliation> scopusAffiliationEvidences = new ArrayList<>();
							if(scopusAuthor != null 
									&& 
									scopusAuthor.getAfids() != null
									&&
									scopusAuthor.getAfids().size() > 0) {
								//Get the matching affiliation ID for target author from scopus and identity affiliation ID and known home institution IDs
								List<Integer> matchingAfids = scopusAuthor.getAfids().stream().distinct().filter(Objects::nonNull).filter(scopusAfid -> this.knownAffiliationIds.contains(String.valueOf(scopusAfid))).collect(Collectors.toList());
								
								if(matchingAfids != null && matchingAfids.size() > 0) {
									//For each match between known affiliation ID and article affiliation create scopusAffiliationEvidence
									for(Integer afid: matchingAfids) {
										TargetAuthorScopusAffiliation scopusAffiliationEvidence = new TargetAuthorScopusAffiliation();
										if(reCiterArticle.getScopusArticle().getAffiliations() != null) {
											Affiliation affiliationScopus = reCiterArticle.getScopusArticle().getAffiliations().stream().filter(affiliation -> affiliation.getAfid() == afid).findFirst().orElse(null);
											if(affiliationScopus != null
													&& 
													affiliationScopus.getAffilname() != null) {
												scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationIdentity(affiliationScopus.getAffilname());
												scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusLabel(affiliationScopus.getAffilname());
											}
											scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.SCOPUS);
											scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusAffiliationId(afid);
											scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.POSITIVE_MATCH_INDIVIDUAL);
											scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveIndividualScore());
											scopusAffiliationEvidences.add(scopusAffiliationEvidence);
											totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveIndividualScore();
										}
									}
									
								} else if(matchingAfids.size() == 0) { //If there is no match then match collaborating institutions, which are defined at the institutional level. Grab values from collaboratingInstitutions-scopusInstitutionIDs (stored in application.properties). Look for overlap between the two.
									matchingAfids = scopusAuthor.getAfids().stream().distinct().filter(Objects::nonNull).filter(scopusAfid -> Arrays.asList(this.collaboratingInstScopusInstitutionsIDs).contains(String.valueOf(scopusAfid))).collect(Collectors.toList());
									if(matchingAfids != null && matchingAfids.size() > 0) {
										//While there can be multiple matches, the maximum score returned for this type of match should be 1.
										int matchCount = 0;
										for(Integer afid: matchingAfids) {
											TargetAuthorScopusAffiliation scopusAffiliationEvidence = new TargetAuthorScopusAffiliation();
											if(reCiterArticle.getScopusArticle().getAffiliations() != null) {
												Affiliation affiliationScopus = reCiterArticle.getScopusArticle().getAffiliations().stream().filter(affiliation -> affiliation.getAfid() == afid).findFirst().orElse(null);
												if(affiliationScopus != null 
														&& 
														affiliationScopus.getAffilname() != null) {
													scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationIdentity(affiliationScopus.getAffilname());
													scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusLabel(affiliationScopus.getAffilname());
												}
												scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.SCOPUS);
												scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusAffiliationId(afid);
												scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.POSITIVE_MATCH_INSTITUTION);
												if(matchCount == 0) {
													scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveInstitutionScore());
													totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveInstitutionScore();
												}
											}
											matchCount++;
											scopusAffiliationEvidences.add(scopusAffiliationEvidence);
										}
									} else {
										TargetAuthorScopusAffiliation scopusAffiliationEvidence = new TargetAuthorScopusAffiliation();
										scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.SCOPUS);
										scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.NO_MATCH);
										if(reCiterArticle.getScopusArticle().getAffiliations() != null 
												&& 
												reCiterArticle.getScopusArticle().getAffiliations().size() > 0) {
											List<Affiliation> scopusAffiliation = reCiterArticle.getScopusArticle().getAffiliations().stream().distinct().filter(affiliation -> scopusAuthor.getAfids().contains(affiliation.getAfid())).collect(Collectors.toList());
											if(scopusAffiliation != null && scopusAffiliation.size() > 0) {
												scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusLabel(scopusAffiliation.get(0).getAffilname());
												scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusAffiliationId(scopusAffiliation.get(0).getAfid());
											}
										}
										scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypeNoMatchScore());
										totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypeNoMatchScore();
										scopusAffiliationEvidences.add(scopusAffiliationEvidence);
									}
								}
							} else if(scopusAuthor != null 
									&& 
									scopusAuthor.getAfids() != null
									&&
									scopusAuthor.getAfids().size() == 0) {
								TargetAuthorScopusAffiliation scopusAffiliationEvidence = new TargetAuthorScopusAffiliation();
								scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.SCOPUS);
								scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.NULL_MATCH);
								scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusLabel(null);
								scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticleScopusAffiliationId(0);
								scopusAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypeNullScore());
								totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypeNullScore();
								scopusAffiliationEvidences.add(scopusAffiliationEvidence);
							}
							
							if(scopusAffiliationEvidences.size() > 0) {
								affiliationEvidence.setScopusTargetAuthorAffiliation(scopusAffiliationEvidences);
								if(reCiterAuthor.getAffiliation() != null) {
									TargetAuthorPubmedAffiliation pubmedAffiliationEvidence = new TargetAuthorPubmedAffiliation();
									pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticlePubmedLabel(reCiterAuthor.getAffiliation());
									affiliationEvidence.setPubmedTargetAuthorAffiliation(pubmedAffiliationEvidence);
								}
							}
						}
					}
					if((affiliationEvidence.getScopusTargetAuthorAffiliation() == null
							||
							(
									affiliationEvidence.getScopusTargetAuthorAffiliation() != null
									&&
									affiliationEvidence.getScopusTargetAuthorAffiliation().stream().allMatch(scopusAffiliation -> scopusAffiliation.getTargetAuthorInstitutionalAffiliationMatchType() == InstitutionalAffiliationMatchType.NULL_MATCH))
							)
							||
							!ReCiterArticleScorer.strategyParameters.isUseScopusArticles()
							) {
						//Evaluate Pubmed
						if(reCiterAuthor.getAffiliation() != null) {
							evaluateTargetAuthorPubmedAffiliation(affiliationEvidence, reCiterAuthor, identity);
						}
						
					}
				} 
			}
			
			if(ReCiterArticleScorer.strategyParameters.isScopusCommonAffiliation()) {
				if(reCiterArticle.getScopusArticle() != null) {
					populateScopusNonTargetAuthorInstitutionsIds(reCiterArticle);
					evaluateNonTargetAuthorScopusAffiliation(affiliationEvidence, reCiterArticle);
				}
			}
			reCiterArticle.setAffiliationEvidence(affiliationEvidence);
			reCiterArticle.setAffiliationScore(this.totalAffiliationScore);
			totalAffiliationScore = 0;
			
			//enable if requrired for debugging
			//slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + affiliationEvidence.toString());
			
			if(this.nonTargetAuthorScopusAffiliationIds.size() > 0) {
				this.nonTargetAuthorScopusAffiliationIds.clear();
			}
			
		}
		return sum;
	}
	
	private void evaluateNonTargetAuthorScopusAffiliation(AffiliationEvidence affiliationEvidence, ReCiterArticle reCiterArticle) {
		Set<String> collaboratingInstScopusInstIds = new HashSet<String>(Arrays.asList(this.collaboratingInstScopusInstitutionsIDs));
		List<Integer> matchingKnownInstitutionIds = null;
		List<Integer> matchingCollaboratingInstituionIds = new ArrayList<>();
		//count of cases where affiliation ID from scopusIDsNonTargetAuthor-Article is in scopusIDsNonTargetAuthor-Identity-KnownInstitutions
		int countScopusIDsNonTargetAuthorArticleKnownInstitution = 0;
		if(this.nonTargetAuthorScopusAffiliationIds.size() > 0 && this.knownAffiliationIds.size() > 0) {
			//countScopusIDsNonTargetAuthorArticleKnownInstitution = (int)this.nonTargetAuthorScopusAffiliationIds.stream().filter(scopusAffiliationId -> this.knownAffiliationIds.contains(String.valueOf(scopusAffiliationId))).count();
			matchingKnownInstitutionIds = this.nonTargetAuthorScopusAffiliationIds.stream().filter(scopusAffiliationId -> this.knownAffiliationIds.contains(String.valueOf(scopusAffiliationId))).collect(Collectors.toList());
			countScopusIDsNonTargetAuthorArticleKnownInstitution = matchingKnownInstitutionIds.size();
		}
		
		int countScopusIDsNonTargetAuthorArticleCollaboratingInstitution = 0;
		if(this.nonTargetAuthorScopusAffiliationIds.size() > 0 && collaboratingInstScopusInstIds.size() > 0) {
			for(Integer scopusAffiliationId: this.nonTargetAuthorScopusAffiliationIds) {
				if(collaboratingInstScopusInstIds.contains(String.valueOf(scopusAffiliationId)) && !matchingKnownInstitutionIds.contains(scopusAffiliationId)) {
					countScopusIDsNonTargetAuthorArticleCollaboratingInstitution++;
					matchingCollaboratingInstituionIds.add(scopusAffiliationId);
				}
			}
			//countScopusIDsNonTargetAuthorArticleCollaboratingInstitution = (int)this.nonTargetAuthorScopusAffiliationIds.stream().filter(scopusAffiliationId -> collaboratingInstScopusInstIds.contains(String.valueOf(scopusAffiliationId))).count();
			//matchingCollaboratingInstituionIds = this.nonTargetAuthorScopusAffiliationIds.stream().filter(scopusAffiliationId -> collaboratingInstScopusInstIds.contains(String.valueOf(scopusAffiliationId))).collect(Collectors.toList());
		}
		
		@AllArgsConstructor
		@Data
		class InstEvidence {
			String scopusAffiliationName;
			int afId;
			int count;
			
			String getscopusAffiliationName() {
				return scopusAffiliationName;
			}
			
			 int getAfId() {
				return afId;
			}
			 
			 int getCount() {
				 return count;
			 }
		}
		
		double overallScore = ReCiterArticleScorer.strategyParameters.getNonTargetAuthorInstAfflMatchTypeMaxScore()
				* ((countScopusIDsNonTargetAuthorArticleKnownInstitution + 
						(countScopusIDsNonTargetAuthorArticleCollaboratingInstitution * ReCiterArticleScorer.strategyParameters.getNonTargetAuthorInstAfflMatchTypeWeight()))/this.nonTargetAuthorScopusAffiliationIds.size());
		
		if(overallScore != 0) {
			NonTargetAuthorScopusAffiliation  nonTargetAuthorScopusAffiliationEvidence = new NonTargetAuthorScopusAffiliation();
			nonTargetAuthorScopusAffiliationEvidence.setNonTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.SCOPUS);
			nonTargetAuthorScopusAffiliationEvidence.setNonTargetAuthorInstitutionalAffiliationScore(AverageClusteringStrategy.roundAvoid(overallScore, 2));
			totalAffiliationScore = totalAffiliationScore + AverageClusteringStrategy.roundAvoid(overallScore, 2);
			if(matchingKnownInstitutionIds != null 
					&&
					matchingKnownInstitutionIds.size() > 0) {
				List<InstEvidence> nonTargetAuthorKnownInstIdsMatch = new ArrayList<InstEvidence>(matchingKnownInstitutionIds.size());
				for(Integer afil : matchingKnownInstitutionIds) {
					Affiliation scopusAffiliation = reCiterArticle.getScopusArticle().getAffiliations().stream().filter(affiliation -> affiliation.getAfid()==afil).findFirst().orElse(null);
					if(scopusAffiliation != null
							&&
							scopusAffiliation.getAffilname() != null) {
						InstEvidence instEvidence = new InstEvidence(scopusAffiliation.getAffilname(), scopusAffiliation.getAfid(), 1);
						nonTargetAuthorKnownInstIdsMatch.add(instEvidence);
					}
				}
				if(nonTargetAuthorKnownInstIdsMatch.size() > 0) {
					Map<String, Long> counting = nonTargetAuthorKnownInstIdsMatch.stream().collect(
			                Collectors.groupingBy(InstEvidence::getscopusAffiliationName, Collectors.counting()));
					List<String> nonTargetAuthorInstIdsMatchString = new ArrayList<String>();
					counting.entrySet().stream().forEach(entry -> {
							int afId = 0;
							for(InstEvidence instEvidence: nonTargetAuthorKnownInstIdsMatch) {
								if(instEvidence.getscopusAffiliationName().equalsIgnoreCase(entry.getKey())) {
									afId = instEvidence.getAfId();
									break;
								}
							}
							String knString = entry.getKey() + ", " + afId + ", " + entry.getValue();
							nonTargetAuthorInstIdsMatchString.add(knString);
					});
					nonTargetAuthorScopusAffiliationEvidence.setNonTargetAuthorInstitutionalAffiliationMatchKnownInstitution(nonTargetAuthorInstIdsMatchString);
				}
 			}
			
			if(matchingCollaboratingInstituionIds != null 
					&&
					matchingCollaboratingInstituionIds.size() > 0) {
				List<InstEvidence> nonTargetAuthorCollabInstIdsMatch = new ArrayList<InstEvidence>(matchingCollaboratingInstituionIds.size());
				for(Integer afil : matchingCollaboratingInstituionIds) {
					Affiliation scopusAffiliation = reCiterArticle.getScopusArticle().getAffiliations().stream().filter(affiliation -> affiliation.getAfid()==afil).findFirst().orElse(null);
					if(scopusAffiliation != null 
							&& 
							scopusAffiliation.getAffilname() != null) {
						//collabInstEvidence = scopusAffiliation.getAffilname() + ", " + scopusAffiliation.getAfid() + ", " + countScopusIDsNonTargetAuthorArticleCollaboratingInstitution;
						//if(!nonTargetAuthorCollabInstIdsMatch.contains(collabInstEvidence)) {
						InstEvidence instEvidence = new InstEvidence(scopusAffiliation.getAffilname(), scopusAffiliation.getAfid(), 1);
							nonTargetAuthorCollabInstIdsMatch.add(instEvidence);
						//}
					}
				}
				if(nonTargetAuthorCollabInstIdsMatch.size() > 0) {
					Map<String, Long> counting = nonTargetAuthorCollabInstIdsMatch.stream().collect(
			                Collectors.groupingBy(InstEvidence::getscopusAffiliationName, Collectors.counting()));
					List<String> nonTargetAuthorCollabInstIdsMatchString = new ArrayList<String>();
					counting.entrySet().stream().forEach(entry -> {
							int afId = 0;
							for(InstEvidence instEvidence: nonTargetAuthorCollabInstIdsMatch) {
								if(instEvidence.getscopusAffiliationName().equalsIgnoreCase(entry.getKey())) {
									afId = instEvidence.getAfId();
									break;
								}
							}
							String collabInstEvidence = entry.getKey() + ", " + afId + ", " + entry.getValue();
							nonTargetAuthorCollabInstIdsMatchString.add(collabInstEvidence);
					});
					nonTargetAuthorScopusAffiliationEvidence.setNonTargetAuthorInstitutionalAffiliationMatchCollaboratingInstitution(nonTargetAuthorCollabInstIdsMatchString);
				}
 			}
			
			affiliationEvidence.setScopusNonTargetAuthorAffiliation(nonTargetAuthorScopusAffiliationEvidence);
		}
	}
	
	/**
	 * This function evaluates pubmed affiliation string for target author with identity as well as home institution keywords and collaborating institution keywords
	 * @param affiliationEvidence The affiliationEvidence object 
	 * @param reCiterAuthor The target author for the article
	 * @param identity The identity related information of the target author stored in SOR
	 */
	private void evaluateTargetAuthorPubmedAffiliation(AffiliationEvidence affiliationEvidence, ReCiterAuthor reCiterAuthor, Identity identity) {
		TargetAuthorPubmedAffiliation pubmedAffiliationEvidence = null;
		String affiliation = reCiterAuthor.getAffiliation().replaceAll(EngineParameters.getRegexForStopWords(), "");
		//Attempt match against identity instituions and if there is a single match then break 
		if(identity.getInstitutions() != null 
				&&
				identity.getInstitutions().size() > 0) {
			for(String identityInst: identity.getInstitutions()) {
				Set<String> santizeInst = new HashSet<String>(Arrays.asList(identityInst.replaceAll(EngineParameters.getRegexForStopWords(), "").split(" ")));
				List<String> matchingKeywords = santizeInst.stream().filter(inst -> StringUtils.containsIgnoreCase(affiliation.trim(), inst.trim())).collect(Collectors.toList());
				if(santizeInst != null 
						&& 
						matchingKeywords != null 
						&& 
						santizeInst.size() == matchingKeywords.size()) {
					pubmedAffiliationEvidence = new TargetAuthorPubmedAffiliation();
					pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationIdentity(identityInst);
					pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticlePubmedLabel(affiliation);
					pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.POSITIVE_MATCH_INDIVIDUAL);
					pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.PUBMED);
					pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveIndividualScore());
					totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveIndividualScore();
					break;
				}
			}
		}
		//If there is not match try with home institutions keywords
		if(pubmedAffiliationEvidence == null) {
			List<String> homeInstKeywords = Arrays.asList(this.homeInstitutionsKeywords);
			if(homeInstKeywords != null 
					&&
					homeInstKeywords.size() > 0) {
				for(String keywords: homeInstKeywords) {
					Set<String> keyword = new HashSet<String>(Arrays.asList(keywords.trim().split("\\|")));
					List<String> matchingKeywords = keyword.stream().filter(inst -> StringUtils.containsIgnoreCase(affiliation, inst)).collect(Collectors.toList());
					if(keyword != null 
							&& 
							matchingKeywords != null 
							&& 
							keyword.size() == matchingKeywords.size()) {
						pubmedAffiliationEvidence = new TargetAuthorPubmedAffiliation();
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationIdentity(ReCiterArticleScorer.strategyParameters.getInstAfflInstLabel());
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticlePubmedLabel(affiliation);
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.POSITIVE_MATCH_INDIVIDUAL);
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.PUBMED);
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveIndividualScore());
						totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveIndividualScore();
						break;
					}
				}
			}
		}
		//If there is still no match try to attempt match using collaborating institutions, which are defined at the institutional level. Grab values from collaboratingInstitutions-keywords (stored in application.properties)
		if(pubmedAffiliationEvidence == null) {
			List<String> collabInstKeywords = Arrays.asList(this.collaboratingInstitutionsKeywords);
			if(collabInstKeywords != null 
					&&
					collabInstKeywords.size() > 0) {
				for(String keywords: collabInstKeywords) {
					List<String> keyword = Arrays.asList(keywords.trim().split("\\|"));
					List<String> matchingKeywords = keyword.stream().filter(inst -> StringUtils.containsIgnoreCase(affiliation.trim(), inst.trim())).collect(Collectors.toList());
					if(keyword != null 
							&& 
							matchingKeywords != null 
							&& 
							keyword.size() == matchingKeywords.size()) {
						pubmedAffiliationEvidence = new TargetAuthorPubmedAffiliation();
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationIdentity(ReCiterArticleScorer.strategyParameters.getInstAfflInstLabel());
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticlePubmedLabel(affiliation);
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.POSITIVE_MATCH_INSTITUTION);
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.PUBMED);
						pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveInstitutionScore());
						totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypePositiveInstitutionScore();
						break;
					}
				}
			}
		}
		if(pubmedAffiliationEvidence == null 
				&&
				affiliation != null) { //There's no match. Output:
			pubmedAffiliationEvidence = new TargetAuthorPubmedAffiliation();
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.PUBMED);
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationIdentity(ReCiterArticleScorer.strategyParameters.getInstAfflInstLabel());
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticlePubmedLabel(affiliation);
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.NO_MATCH);
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypeNoMatchScore());
			totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getNonTargetAuthorInstAfflMatchTypeNoMatchScore();
		}
		if(pubmedAffiliationEvidence == null
				&&
				affiliation == null) {
			pubmedAffiliationEvidence = new TargetAuthorPubmedAffiliation();
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationSource(InstitutionalAffiliationSource.PUBMED);
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationIdentity(ReCiterArticleScorer.strategyParameters.getInstAfflInstLabel());
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationArticlePubmedLabel(null);
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchType(InstitutionalAffiliationMatchType.NULL_MATCH);
			pubmedAffiliationEvidence.setTargetAuthorInstitutionalAffiliationMatchTypeScore(ReCiterArticleScorer.strategyParameters.getTargetAuthorInstAfflMatchTypeNullScore());
			totalAffiliationScore = totalAffiliationScore + ReCiterArticleScorer.strategyParameters.getNonTargetAuthorInstAfflMatchTypeNoMatchScore();
		}
		affiliationEvidence.setPubmedTargetAuthorAffiliation(pubmedAffiliationEvidence);
	}
	
	/**
	 * This function contains all scopusInstitutionIDs (e.g., 60007997) from article.affiliation for all nonTargetAuthors.
	 * @param reCiterArticle
	 */
	private void populateScopusNonTargetAuthorInstitutionsIds(ReCiterArticle reCiterArticle) {
		for(ReCiterAuthor reCiterAuthor: reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if(!reCiterAuthor.isTargetAuthor()) {
				Author scopusAuthor = reCiterArticle.getScopusArticle().getAuthors().stream().filter(author -> reCiterAuthor.getRank() == author.getSeq()).findFirst().orElse(null);
				if(scopusAuthor != null
						&& scopusAuthor.getAfids() != null) {
					this.nonTargetAuthorScopusAffiliationIds.addAll(scopusAuthor.getAfids().stream().distinct().collect(Collectors.toList()));
				}
			}
		}
	}
	
	/**
	 * This function gets institutions from Identity sources and Scopus home Institutions IDs if declared in application.properties and return a unique set of knownAffiliationIDs
	 * @param identity
	 */
	private void populateKnownAffiliationIds(Identity identity) {
		if(identity.getInstitutions() != null 
				&&
				identity.getInstitutions().size() > 0) {
			//Map<String, List<String>> afidTomap = EngineParameters.getAfiliationNameToAfidMap();
			//identity.getInstitutions().stream().forEach(institutions -> {
			for(String institutions: identity.getInstitutions()) {
				if(EngineParameters.getAfiliationNameToAfidMap() != null 
						&& 
						EngineParameters.getAfiliationNameToAfidMap().containsKey(institutions.trim())
						) {
					this.knownAffiliationIds.addAll(EngineParameters.getAfiliationNameToAfidMap().get(institutions.trim()));
				}
			}
		}
		
		if(this.homeInstScopusInstitutionsIDs.length > 0) {
			this.knownAffiliationIds.addAll(Arrays.asList(this.homeInstScopusInstitutionsIDs));
		}
	}
	
	/**
	 * Check if the ReCiterArticle's affiliation information contains the phrase 
	 * "weill cornell", "weill-cornell", "weill medical" using case-insensitive
	 * string matching.
	 * 
	 * @param reCiterArticle
	 * @return
	 */
	protected boolean containsWeillCornell(ReCiterArticle reCiterArticle) {
		for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if (author.getAffiliation() != null) {
				String affiliation = author.getAffiliation();
				List<String> affiliationsMatches = containsWeillCornell(affiliation);
				if (affiliationsMatches.size() > 0) {
					reCiterArticle.setFrequentInstitutionalCollaborators(affiliationsMatches);
					String variantName = affiliation;
					return true;
				}
			}
		}
		return false;
	}
	
	protected List<String> containsWeillCornell(String affiliation) {
		List<String> affiliationsMatches = new ArrayList<String>();
		
		if (StringUtils.containsIgnoreCase(affiliation, "weill cornell")) {
			affiliationsMatches.add("weill cornell");
		}
		
		if (StringUtils.containsIgnoreCase(affiliation, "weill-cornell")) {
			affiliationsMatches.add("weill-cornell");
		}
		
		if (StringUtils.containsIgnoreCase(affiliation, "weill medical")) {
			affiliationsMatches.add("weill medical");
		}
		
		if (StringUtils.containsIgnoreCase(affiliation, "cornell medical center")) {
			affiliationsMatches.add("cornell medical center");
		}
		
		if (StringUtils.containsIgnoreCase(affiliation, "Memorial Sloan-Kettering Cancer Center")) {
			affiliationsMatches.add("Memorial Sloan-Kettering Cancer Center");
		}
		
		if (StringUtils.containsIgnoreCase(affiliation, "Sloan-Kettering")) {
			affiliationsMatches.add("Sloan-Kettering");
		}
		
		if (StringUtils.containsIgnoreCase(affiliation, "Sloan Kettering")) {
			affiliationsMatches.add("Sloan Kettering");
		}
		
		return affiliationsMatches;
	}

	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		if (containsWeillCornell(reCiterArticle)) {
			feature.setWeillCornellAffiliation(1);
		}
	}
}
