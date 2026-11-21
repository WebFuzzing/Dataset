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
package reciter.xml.retriever.pubmed;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.xml.retriever.pubmed.PubMedQueryType.PubMedQueryBuilder;

/**
 * @author szd2013
 * This class gets all the homeInstitutions defined in application property and forms a search string for pubmed
 */
@Component("affiliationRetrievalStrategy")
public class AffiliationRetrievalStrategy extends AbstractNameRetrievalStrategy {

	private static final String retrievalStrategyName = "AffiliationRetrievalStrategy";
	//private static final String AFFILIATION_QUERY = "AND ((new york) OR 10065 OR 10021 OR weill OR cornell OR (newyork AND presbyterian) OR (new york AND presbyterian) OR HSS OR (hospital special surgery) OR (North Shore hospital) OR (Long Island Jewish) OR (memorial sloan) OR (sloan kettering) OR sloan-kettering OR hamad OR (mount sinai) OR (methodist houston) OR (National Institute of Mental Health) OR (beth israel) OR (University of Pennsylvania Medicine) OR (Merck Research) OR (New York Medical College) OR (Medicine Dentistry New Jersey) OR Montefiore OR (Lenox Hill) OR (Cold Spring Harbor) OR (St. Luke's-Roosevelt) OR (New York University Medicine) OR Langone OR (SUNY Downstate) OR (Albert Einstein Medicine) OR Yeshiva OR UMDNJ OR Icahn Medicine OR (Mount Sinai) OR (columbia medical) OR (columbia physicians))";
	
	@Value("${strategy.authorAffiliationScoringStrategy.homeInstitution-keywords}")
	private String homeInstitutionsKeywords;
	

	@Override
	public String getRetrievalStrategyName() {
		return retrievalStrategyName;
	}

	@Override
	protected String getStrategySpecificKeyword(Identity identity) {
		return getHomeInstitutionsForPubmed();
	}

	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(true, false, identitynames);
		
		return pubMedQueryBuilder.build();
	}
	
	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity, Date startDate,
			Date endDate) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder(getStrategySpecificKeyword(identity))
					.author(true, false, identitynames)
					.dateRange(true, startDate, endDate);
		
		return pubMedQueryBuilder.build();
	}
	
	/**
	 * @return returns the string search for pubmed
	 */
	public String getHomeInstitutionsForPubmed() {
		List<String> homeInstKeywords = Arrays.asList(homeInstitutionsKeywords.trim().split("\\s*,\\s*"));
		StringBuilder affiliationQueryString = new StringBuilder();
		affiliationQueryString.append("(");
		for(String keywords: homeInstKeywords) {
			List<String> keyword = Arrays.asList(keywords.trim().split("\\|"));
			if(keyword.size() == 1) {
				affiliationQueryString.append(keyword.get(0) + "[affiliation]");
			} else {
				affiliationQueryString.append("(");
				for(String word: keyword) {
					affiliationQueryString.append(word + "[affiliation]");
					if(!word.equals(keyword.get(keyword.size() - 1))) {//Check for last element
						affiliationQueryString.append(" AND ");
					}
				}
				affiliationQueryString.append(")");
			}
			if(!keywords.equals(homeInstKeywords.get(homeInstKeywords.size() - 1))) { //Check for last element
				affiliationQueryString.append(" OR ");
			}
		}
		affiliationQueryString.append(")");
		
		return affiliationQueryString.toString();
		
	}
}
