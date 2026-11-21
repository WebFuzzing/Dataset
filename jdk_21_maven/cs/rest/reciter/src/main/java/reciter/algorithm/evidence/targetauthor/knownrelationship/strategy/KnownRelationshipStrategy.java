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
package reciter.algorithm.evidence.targetauthor.knownrelationship.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.Feature;
import reciter.engine.analysis.evidence.RelationshipEvidence;
import reciter.engine.analysis.evidence.RelationshipNegativeMatch;
import reciter.engine.analysis.evidence.RelationshipPostiveMatch;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;
import reciter.model.identity.KnownRelationship;
import reciter.model.identity.KnownRelationship.RelationshipType;

public class KnownRelationshipStrategy extends AbstractTargetAuthorStrategy {
	
	private static final Logger log = LoggerFactory.getLogger(KnownRelationshipStrategy.class);

	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		double score = 0;
		/*List<KnownRelationship> relationships = identity.getKnownRelationships();
		List<RelationshipEvidence> relationshipEvidences = new ArrayList<>();
		if (relationships != null) {
			for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
				// do not match target author's name
				if (!author.getAuthorName().firstInitialLastNameMatch(identity.getPrimaryName())) {
					for (KnownRelationship authorName : relationships) {
						if (authorName.getName().isFullNameMatch(author.getAuthorName())) {
							log.info("[known relationship match: " +  authorName + "] ");
							reCiterArticle.getKnownRelationships().add(author);
							reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + "[known relationship match: " +  authorName + "] ");
							score += 1;
							reCiterArticle.getKnownRelationship().add(authorName);
							RelationshipEvidence relationshipEvidence = new RelationshipEvidence();
							relationshipEvidence.setRelationshipName(authorName.getName());
							relationshipEvidence.setRelationshipType(authorName.getType());
							relationshipEvidences.add(relationshipEvidence);
						}
					}
				}
			}
			reCiterArticle.setKnownCoinvestigatorScore(score);
		}
		reCiterArticle.setRelationshipEvidence(relationshipEvidences);*/
		return score;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		double sum = 0;
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			long relationShipMatchCount = 0;
			long nonMatchCount = 0;
			long identityRelationShipCount = 0;
			final long[] relationShipVerboseMatchCount = {0};
			
			//sum += executeStrategy(reCiterArticle, identity);
			List<KnownRelationship> relationships = new ArrayList<KnownRelationship>();
			if(identity.getKnownRelationships() != null) {
				relationships = identity.getKnownRelationships();
			}
			RelationshipEvidence relaEvidence = new RelationshipEvidence();
			RelationshipNegativeMatch relationshipNegativeMatch = new RelationshipNegativeMatch();
			List<RelationshipPostiveMatch> relationshipEvidences = new ArrayList<>();
			
			if (relationships != null && !relationships.isEmpty()) {
				for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
					Set<String> relationshipTypes = new HashSet<String>();
					// do not match target author's name
					if (!author.isTargetAuthor()) {
						for (KnownRelationship authorName : relationships) {
							if (authorName.getName().firstInitialLastNameMatch(author.getAuthorName())) {
								RelationshipPostiveMatch relationshipEvidence = new RelationshipPostiveMatch();
								//if(StringUtils.equalsIgnoreCase(authorName.getName().getFirstName(), author.getAuthorName().getFirstName())) {
								if(authorName.getName().getFirstName().length() > 1 
										&&
										author.getAuthorName().getFirstName().startsWith(authorName.getName().getFirstName())) {
									relationshipEvidence.setRelationshipMatchType("verbose");
									relationshipEvidence.setRelationshipVerboseMatchModifierScore(ReCiterArticleScorer.strategyParameters.getRelationshipVerboseMatchModifier());
									relationShipVerboseMatchCount[0]++;
								} else {
									relationshipEvidence.setRelationshipMatchType("initial");
								}
								//log.info("[known relationship match: " +  authorName + "] ");
								reCiterArticle.getKnownRelationships().add(author);
								reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + "[known relationship match: " +  authorName + "] ");
								sum += 1;
								reCiterArticle.getKnownRelationship().add(authorName);
								relationshipEvidence.setRelationshipMatchingScore(ReCiterArticleScorer.strategyParameters.getRelationshipMatchingScore());
								relationshipEvidence.setRelationshipNameArticle(author.getAuthorName());
								relationshipEvidence.setRelationshipNameIdentity(authorName.getName());
								relationshipEvidence.setRelationshipType(relationshipTypes);
								
								if(authorName.getType() == RelationshipType.MENTOR) {
									relationshipEvidence.setRelationshipMatchModifierMentor(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierMentor());
									if(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() > 0 
											&& 
											author.getAuthorName().equals(reCiterArticle.getArticleCoAuthors().getAuthors().get(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() - 1).getAuthorName())
											) { //If the matching author is the last author or senior author
										relationshipEvidence.setRelationshipMatchModifierMentorSeniorAuthor(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierMentorSeniorAuthor());
									}
								}
								
								if(authorName.getType() == RelationshipType.MANAGER) {
									relationshipEvidence.setRelationshipMatchModifierManager(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierManager());
									if(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() > 0 
											&& 
											author.getAuthorName().equals(reCiterArticle.getArticleCoAuthors().getAuthors().get(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() - 1).getAuthorName())
											) { //If the matching author is the manager and the last author or senior author
										relationshipEvidence.setRelationshipMatchModifierManagerSeniorAuthor(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierManagerSeniorAuthor());
									}
								}
								
								if(relationshipEvidences.size() > 0 
										&&
										relationshipEvidences.stream().anyMatch(evidence -> authorName.getName().getFirstName().equalsIgnoreCase(evidence.getRelationshipNameIdentity().getFirstName())
												&&
												authorName.getName().getLastName().equalsIgnoreCase(evidence.getRelationshipNameIdentity().getLastName()))) {
									RelationshipPostiveMatch relationshipEvidenceInList = relationshipEvidences.stream().filter(evidence -> authorName.getName().getFirstName().equalsIgnoreCase(evidence.getRelationshipNameIdentity().getFirstName())
												&&
												authorName.getName().getLastName().equalsIgnoreCase(evidence.getRelationshipNameIdentity().getLastName())
												).findFirst().get();
										
										if(relationshipEvidenceInList != null) {
											if(authorName.getType() == RelationshipType.MENTOR) {
												relationshipEvidenceInList.setRelationshipMatchModifierMentor(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierMentor());
												if(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() > 0 
														&& 
														author.getAuthorName().equals(reCiterArticle.getArticleCoAuthors().getAuthors().get(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() - 1).getAuthorName())
														) { //If the matching author is the last author or senior author
													relationshipEvidenceInList.setRelationshipMatchModifierMentorSeniorAuthor(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierMentorSeniorAuthor());
												}
											}
											
											if(authorName.getType() == RelationshipType.MANAGER) {
												relationshipEvidenceInList.setRelationshipMatchModifierManager(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierManager());
												if(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() > 0 
														&& 
														author.getAuthorName().equals(reCiterArticle.getArticleCoAuthors().getAuthors().get(reCiterArticle.getArticleCoAuthors().getNumberOfAuthors() - 1).getAuthorName())
														) { //If the matching author is the manager and the last author or senior author
													relationshipEvidenceInList.setRelationshipMatchModifierManagerSeniorAuthor(ReCiterArticleScorer.strategyParameters.getRelationshipMatchModifierManagerSeniorAuthor());
												}
											}
											if(authorName.getType() != null) {
												relationshipEvidenceInList.getRelationshipType().add(authorName.getType().toString());
											}
											continue;
										}
								} else {
									if(authorName.getType() != null) {
										relationshipTypes.add(authorName.getType().toString());
									}
								}
								relationShipMatchCount++;
								relationshipEvidences.add(relationshipEvidence);
							}
						}
					}
					
				}
				nonMatchCount = reCiterArticle.getArticleCoAuthors().getAuthors().stream().filter(author -> !author.isTargetAuthor()).count() - relationShipVerboseMatchCount[0];
				reCiterArticle.setKnownCoinvestigatorScore(sum);
			}
			relationshipEvidences.stream().forEach(relationShipEvidence -> relationShipEvidence.setRelationshipMatchingCount(relationShipVerboseMatchCount[0]));
			identityRelationShipCount = Optional.ofNullable(identity.getKnownRelationships()).map(relations -> relations.stream().distinct().count()).orElse(0L);
			if(relationShipVerboseMatchCount!=null && relationShipVerboseMatchCount.length > 0)
			{
				double relationshipPositiveMatchingScore = Math.pow((relationShipVerboseMatchCount[0] > 0 ? relationShipVerboseMatchCount[0]+ 1 : 1) / (double)(identityRelationShipCount > 0 ? identityRelationShipCount + 1 : 1), 0.5);
				//relationshipEvidences.stream().forEach(relationShipEvidence -> relationShipEvidence.setRelationshipMatchingScore(relationshipPositiveMatchingScore));
				
				relaEvidence.setRelationshipPositiveMatchScore(relationshipPositiveMatchingScore);
			}
			double relationshipNegativeMatchingScore = Math.pow((nonMatchCount > 0 ? nonMatchCount+ 1 : 1) / (double)(identityRelationShipCount > 0 ? identityRelationShipCount + 1 : 1), 0.5);
			//relationshipEvidences.stream().forEach(relationShipEvidence -> relationShipEvidence.setRelationshipMatchingScore(relationshipNegativeMatchingScore));
			
			relaEvidence.setRelationshipNegativeMatchScore(relationshipNegativeMatchingScore);
			relaEvidence.setRelationshipIdentityCount(identityRelationShipCount);
			
			relaEvidence.setRelationshipPositiveMatch(relationshipEvidences);
			relationshipNegativeMatch.setRelationshipNonMatchCount(nonMatchCount);
			relationshipNegativeMatch.setRelationshipMinimumTotalScore(ReCiterArticleScorer.strategyParameters.getRelationshipMinimumTotalScore());
			relationshipNegativeMatch.setRelationshipNonMatchScore(ReCiterArticleScorer.strategyParameters.getRelationshipNonMatchScore());
			double totalRelationshipScore = relationshipEvidences.stream().mapToDouble(relationShipEvidence -> relationShipEvidence.getRelationshipMatchingScore() 
					+ relationShipEvidence.getRelationshipVerboseMatchModifierScore()
					+ relationShipEvidence.getRelationshipMatchModifierMentorSeniorAuthor()
					+ relationShipEvidence.getRelationshipMatchModifierMentor()
					+ relationShipEvidence.getRelationshipMatchModifierManagerSeniorAuthor()
					+ relationShipEvidence.getRelationshipMatchModifierManager()).sum();
			totalRelationshipScore = totalRelationshipScore + (nonMatchCount * ReCiterArticleScorer.strategyParameters.getRelationshipNonMatchScore());
			/*if(totalRelationshipScore <= ReCiterArticleScorer.strategyParameters.getRelationshipMinimumTotalScore()) {
				relaEvidence.setRelationshipEvidenceTotalScore(ReCiterArticleScorer.strategyParameters.getRelationshipMinimumTotalScore());
			} else {
				relaEvidence.setRelationshipEvidenceTotalScore(BigDecimal.valueOf(totalRelationshipScore).setScale(2, RoundingMode.HALF_DOWN).doubleValue());
			}*/
			//relaEvidence.setRelationshipNegativeMatch(relationshipNegativeMatch);
			
			reCiterArticle.setRelationshipEvidence(relaEvidence);
			//enable if requrired for debugging
			//log.info("Pmid: " + reCiterArticle.getArticleId() + " " + relaEvidence.toString());
		}
		return sum;
	}

	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		int score = 0;
		List<KnownRelationship> relationships = identity.getKnownRelationships();

		if (relationships != null) {
			for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
				// do not match target author's name
				if (!author.getAuthorName().firstInitialLastNameMatch(identity.getPrimaryName())) {
					for (KnownRelationship authorName : relationships) {
						if (authorName.getName().isFullNameMatch(author.getAuthorName())) {
							score += 1;
						}
					}
				}
			}
			reCiterArticle.setKnownCoinvestigatorScore(score);
		}
		feature.setNumKnownRelationships(score);
	}

}
