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
 ******************************************************************************/
package reciter.xml.retriever.pubmed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.xml.retriever.engine.AliasReCiterRetrievalEngine.IdentityNameType;
import reciter.xml.retriever.pubmed.PubMedQueryType.PubMedQueryBuilder;

/**
 * Retrieves PubMed articles using the ORCID [auid] search qualifier.
 * Reads the ORCID directly from Identity (thread-safe — no mutable state on this singleton).
 * Lenient and strict queries are identical because [auid] is inherently precise.
 */
@Component("orcidRetrievalStrategy")
public class OrcidRetrievalStrategy extends AbstractRetrievalStrategy {

	private static final String retrievalStrategyName = "OrcidRetrievalStrategy";

	@Override
	public String getRetrievalStrategyName() {
		return retrievalStrategyName;
	}

	private String constructOrcidQuery(Identity identity) {
		String orcid = identity.getOrcid();
		if (orcid != null && !orcid.isBlank()) {
			return orcid.trim() + "[auid]";
		}
		return null;
	}

	@Override
	protected List<PubMedQueryType> buildQuery(Identity identity,
			Map<IdentityNameType, Set<AuthorName>> identityNames) {
		List<PubMedQueryType> pubMedQueries = new ArrayList<PubMedQueryType>();

		String queryString = constructOrcidQuery(identity);
		if (queryString == null) {
			return pubMedQueries;
		}

		PubMedQueryBuilder pubMedQueryBuilder = new PubMedQueryBuilder(queryString);
		PubMedQuery orcidQuery = pubMedQueryBuilder.build();

		PubMedQueryType pubMedQueryType = new PubMedQueryType();
		pubMedQueryType.setLenientQuery(new PubMedQueryResult(orcidQuery));
		pubMedQueryType.setStrictQuery(new PubMedQueryResult(orcidQuery));
		pubMedQueryType.setLenientCountQuery(new PubMedQueryResult(orcidQuery));
		pubMedQueryType.setStrictCountQuery(new PubMedQueryResult(orcidQuery));
		pubMedQueries.add(pubMedQueryType);

		return pubMedQueries;
	}

	@Override
	protected List<PubMedQueryType> buildQuery(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames,
			Date startDate, Date endDate) {
		List<PubMedQueryType> pubMedQueries = new ArrayList<PubMedQueryType>();

		String queryString = constructOrcidQuery(identity);
		if (queryString == null) {
			return pubMedQueries;
		}

		PubMedQueryBuilder pubMedQueryBuilder = new PubMedQueryBuilder(queryString).dateRange(true,
				startDate, endDate);
		PubMedQuery orcidQuery = pubMedQueryBuilder.build();

		PubMedQueryBuilder pubMedQueryBuilderCount = new PubMedQueryBuilder(queryString);
		PubMedQuery orcidQueryCount = pubMedQueryBuilderCount.build();

		PubMedQueryType pubMedQueryType = new PubMedQueryType();
		pubMedQueryType.setLenientQuery(new PubMedQueryResult(orcidQuery));
		pubMedQueryType.setStrictQuery(new PubMedQueryResult(orcidQuery));
		pubMedQueryType.setLenientCountQuery(new PubMedQueryResult(orcidQueryCount));
		pubMedQueryType.setStrictCountQuery(new PubMedQueryResult(orcidQueryCount));
		pubMedQueries.add(pubMedQueryType);

		return pubMedQueries;
	}

	@Override
	public RetrievalResult retrievePubMedArticles(List<Long> pmids) throws IOException {
		throw new UnsupportedOperationException("Does not support retrieval by pmids.");
	}
}
