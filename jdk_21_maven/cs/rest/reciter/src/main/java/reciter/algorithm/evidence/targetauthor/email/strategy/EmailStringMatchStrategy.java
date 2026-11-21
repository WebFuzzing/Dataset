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
package reciter.algorithm.evidence.targetauthor.email.strategy;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.cluster.similarity.clusteringstrategy.article.EmailFeatureClusteringStrategy;
import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.Feature;
import reciter.engine.analysis.evidence.EmailEvidence;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class EmailStringMatchStrategy extends AbstractTargetAuthorStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(EmailStringMatchStrategy.class);
	private List<String> emailSuffixes;
	
	private final String[] defaultSuffixes = ReCiterArticleScorer.strategyParameters.getDefaultSuffixes().trim().split(",");
	
	public EmailStringMatchStrategy() {
		setEmailSuffixes(Arrays.asList(defaultSuffixes));
	}
	
	public EmailStringMatchStrategy(List<String> emailSuffixes) {
		this.setEmailSuffixes(emailSuffixes);
	}
	
	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		double score = 0;
		for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if (author.getAffiliation() != null && author.getAffiliation() != null) {
				String affiliation = author.getAffiliation();
				
				for (String suffix : emailSuffixes) {
					String email = identity.getUid() + suffix;
					if (StringUtils.containsIgnoreCase(affiliation, email)) {
						reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + " [email matches: " + affiliation + "]");
						reCiterArticle.getMatchingEmails().add(email);
						score += 1;
					}
				}
				
				// TODO output features.

				for (String email : identity.getEmails()) {
					if (affiliation.contains(email)) {
						reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + " [email matches: " + email + "]");
						reCiterArticle.getMatchingEmails().add(email);
						score += 1;
					}
				}
			}
		}
		reCiterArticle.setEmailStrategyScore(score);
		return score;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		double sumScore = 0;
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			//sumScore += executeStrategy(reCiterArticle, identity);
			for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
				EmailEvidence emailEvidence = new EmailEvidence();
				if (author.isTargetAuthor() && author.getAffiliation() != null) {
					String affiliation = author.getAffiliation();
					for (String email : identity.getEmails()) {
						if (affiliation.toLowerCase().contains(email.toLowerCase())) {
							reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + " [email matches: " + email + "]");
							reCiterArticle.getMatchingEmails().add(email);
							emailEvidence.setEmailMatch(email);
							emailEvidence.setEmailMatchScore(ReCiterArticleScorer.strategyParameters.getEmailMatchScore());
							break;
						} else if(emailSuffixes.stream().anyMatch(suffix -> affiliation.toLowerCase().contains(identity.getUid().toLowerCase() + suffix.toLowerCase()))) {
							reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + " [email matches: " + affiliation + "]");
							reCiterArticle.getMatchingEmails().add(email);
							emailEvidence.setEmailMatch(email);
							emailEvidence.setEmailMatchScore(ReCiterArticleScorer.strategyParameters.getEmailMatchScore());
							break;
						} else {
							emailEvidence.setEmailMatch(EmailFeatureClusteringStrategy.sanitizeAffiliation(affiliation));
							emailEvidence.setEmailMatchScore(ReCiterArticleScorer.strategyParameters.getEmailNoMatchScore());
						}
					}
				}
				if(emailEvidence.getEmailMatch() != null) {
					reCiterArticle.setEmailEvidence(emailEvidence);
					slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + emailEvidence.toString());
					break;
				}
			}
		}
		return sumScore;
	}
	
	/**
	 * @med.cornell.edu", "@mail.med.cornell.edu", "@weill.cornell.edu", "@nyp.org
	 */
	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		for (ReCiterAuthor author : reCiterArticle.getArticleCoAuthors().getAuthors()) {
			if (author.getAffiliation() != null && author.getAffiliation() != null) {
				String affiliation = author.getAffiliation();
				if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[0])) {
					feature.setMedCornellEdu(1);
				} else if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[1])) {
					feature.setMailMedCornellEdu(1);
				} else if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[2])) {
					feature.setWeillCornellEdu(1);
				} else if (StringUtils.containsIgnoreCase(affiliation, identity.getUid() + defaultSuffixes[3])) {
					feature.setNypOrg(1);
				}
			}
		}
	}

	public String[] getDefaultSuffixes() {
		return defaultSuffixes;
	}

	public List<String> getEmailSuffixes() {
		return emailSuffixes;
	}

	public void setEmailSuffixes(List<String> emailSuffixes) {
		this.emailSuffixes = emailSuffixes;
	}
}
