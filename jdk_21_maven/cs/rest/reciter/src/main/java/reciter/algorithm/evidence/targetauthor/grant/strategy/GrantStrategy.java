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
package reciter.algorithm.evidence.targetauthor.grant.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.evidence.targetauthor.AbstractTargetAuthorStrategy;
import reciter.engine.Feature;
import reciter.engine.analysis.evidence.Grant;
import reciter.engine.analysis.evidence.GrantEvidence;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleGrant;
import reciter.model.identity.Identity;

public class GrantStrategy extends AbstractTargetAuthorStrategy {

	private static final Logger log = LoggerFactory.getLogger(GrantStrategy.class);

	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		log.info("Executing grant strategy for article id {} and identity id {}",
				reCiterArticle.getArticleId(),
				identity.getUid());
		double score = 0;
		GrantEvidence grantEvidence = new GrantEvidence();
		List<Grant> grants = new ArrayList<>();
		grantEvidence.setGrants(grants);
		if (reCiterArticle.getGrantList() == null) {
			reCiterArticle.setGrantEvidence(grantEvidence);
			return 0;
		}
		for (ReCiterArticleGrant grant : reCiterArticle.getGrantList()) {
			for (String identityGrantId : identity.getGrants()) {
				log.info("identity grant {}", identityGrantId);
				if (grant.getGrantID() != null && grant.getGrantID().contains(identityGrantId)) {
					log.info("[known grant ids match=" + identityGrantId + "]");
					reCiterArticle.setClusterInfo(reCiterArticle.getClusterInfo() + " [known grant ids match=" + identityGrantId + "], ");
					Grant analysisGrant = new Grant();
					analysisGrant.setArticleGrant(grant.getGrantID());
					analysisGrant.setInstitutionGrant(identityGrantId);
					score += 1;
					reCiterArticle.getMatchingGrantList().add(grant);
					grants.add(analysisGrant);
				}
			}
		}
		reCiterArticle.setGrantEvidence(grantEvidence);
		return score;
	}
	
	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		Set<String> sanitizedIdentityGrants = new HashSet<String>();
		if(identity != null) {
			sanitizeIdentityGrants(identity, sanitizedIdentityGrants);
		}
		GrantEvidence grantEvidence = null;
		
		double score = 0;
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			//score += executeStrategy(reCiterArticle, identity);
			List<Grant> grants = new ArrayList<>();
			checkForValidGrant(reCiterArticle); // Calling here as we removed the old clustering logic for the Grants
			if (reCiterArticle.getGrantList() == null) continue;
			for (ReCiterArticleGrant grant : reCiterArticle.getGrantList()) {
				for (String identityGrantId : sanitizedIdentityGrants) {
					//log.info("identity grant {}", identityGrantId);
					if (grant.getGrantID() != null && org.apache.commons.lang3.StringUtils.equalsIgnoreCase(grant.getSanitizedGrantID(), identityGrantId)) {
						Grant analysisGrant = new Grant();
						analysisGrant.setArticleGrant(grant.getGrantID());
						analysisGrant.setInstitutionGrant(identityGrantId);
						analysisGrant.setGrantMatchScore(ReCiterArticleScorer.strategyParameters.getGrantMatchScore());
						score += 1;
						reCiterArticle.getMatchingGrantList().add(grant);
						if(!grants.stream().anyMatch(matchingGrant -> org.apache.commons.lang3.StringUtils.equalsIgnoreCase(identityGrantId, matchingGrant.getInstitutionGrant()))) {
							grants.add(analysisGrant);
						}
						
						
						
					}
				}
			}
			grantEvidence = new GrantEvidence();
			grantEvidence.setGrants(grants);
			if(grantEvidence != null && grantEvidence.getGrants().size() > 0) {
				log.info("Pmid: " + reCiterArticle.getArticleId() + " " + grantEvidence.toString());
				reCiterArticle.setGrantEvidence(grantEvidence);
			}
		}
		return score;
	}

	@Override
	public void populateFeature(ReCiterArticle reCiterArticle, Identity identity, Feature feature) {
		// TODO Auto-generated method stub
		
	}
	
	private void sanitizeIdentityGrants(Identity identity, Set<String> sanitizedIdentityGrants) {
		if(identity.getGrants() != null && !identity.getGrants().isEmpty()) {
			for (String identityGrantId : identity.getGrants()) {
				// Identity feeds carry blank and all-zero grant ids; insert(2, "-") below throws on
				// anything under 2 chars and that kills grant evidence for the whole person.
				if (identityGrantId == null || identityGrantId.trim().length() < 3 || identityGrantId.matches("^0+$")) {
					continue;
				}
				//Remove leading zeroes
				//Paul confirmed identity grants(NIH) always starts with alphabets with numbers so excluding the possibility of grants with numbers and leading zeroes e.g. 0012301 
				if(identityGrantId.matches("^(?i)[A-Z]+0+.*$")) { //This is checking if grant starts with Alphabets with 0 e.g. DP001021 this will be true but not for DP11201
					int zeroIndex = identityGrantId.indexOf("0");
					String grantId = identityGrantId.substring(zeroIndex, identityGrantId.length()).replaceAll("^[0]+", "");
					//identityGrantId.replaceAll("^[A-Z0]+(?!$)/i/g", "");
					String stripped = identityGrantId.substring(0, zeroIndex) + grantId;
					if (stripped.length() >= 2) {
						sanitizedIdentityGrants.add(new StringBuilder(stripped).insert(2, "-").toString());
					}
				} else {
					sanitizedIdentityGrants.add(new StringBuilder(identityGrantId).insert(2, "-").toString());
				}
			}
		}
	}
	private void checkForValidGrant(ReCiterArticle reCiterArticle) {
		if (reCiterArticle.getGrantList() == null) return;
		for (ReCiterArticleGrant grant : reCiterArticle.getGrantList()) {
			if(grant.getGrantID() != null) {
				String sanitizedGrant = sanitizeGrant(grant.getGrantID().replaceAll("[\\s\\-]", ""));
				if(sanitizedGrant != null) {
					grant.setSanitizedGrantID(sanitizedGrant);
				}
			}
		}	
	}
	private String sanitizeGrant(String grant) {
		String fundingAgency = null;
		String grantId = null;
		Pattern pattern = Pattern.compile("([a-zA-Z][a-zA-Z]+)");
		Matcher matcher = pattern.matcher(grant);
		int matchCount = 0;
		while(matcher.find()) {
			matchCount++;
			if(matchCount == 2) {
				fundingAgency = matcher.group();
			}
			fundingAgency = matcher.group();
		}
		
		matchCount = 0;
		pattern = Pattern.compile("[0-9]{4,6}");
		matcher = pattern.matcher(grant);
		while(matcher.find()) {
			grantId = matcher.group().replaceFirst("^0*", "");
		}
		
		if(fundingAgency != null && grantId != null) {
			return fundingAgency + "-" + grantId;
		}
		return null;
	}
}
