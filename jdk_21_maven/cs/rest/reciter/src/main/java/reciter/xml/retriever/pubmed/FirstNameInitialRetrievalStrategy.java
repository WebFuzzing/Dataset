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

import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.xml.retriever.pubmed.PubMedQueryType.PubMedQueryBuilder;

@Component("firstNameInitialRetrievalStrategy")
public class FirstNameInitialRetrievalStrategy extends AbstractNameRetrievalStrategy {

	private static final String retrievalStrategyName = "FirstNameInitialRetrievalStrategy";
	private final static Logger slf4jLogger = LoggerFactory.getLogger(FirstNameInitialRetrievalStrategy.class);

	@Override
	public String getRetrievalStrategyName() {
		return retrievalStrategyName;
	}

	@Override
	protected String getStrategySpecificKeyword(Identity identity) {
		return null;
	}

	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder()
					.author(true, false, identitynames);
		
		PubMedQuery query = pubMedQueryBuilder.build();
		slf4jLogger.info(retrievalStrategyName + " produced query=[" + query + "]");
		return query;
	}
	
	@Override
	protected PubMedQuery buildNameQuery(Set<AuthorName> identitynames, Identity identity, Date startDate,
			Date endDate) {
		PubMedQueryBuilder pubMedQueryBuilder = 
				new PubMedQueryBuilder()
					.author(true, false, identitynames)
					.dateRange(true, startDate, endDate);
		
		return pubMedQueryBuilder.build();
	}
}
