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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reciter.database.dynamodb.model.GoldStandard;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.service.dynamo.IDynamoDbGoldStandardService;
import reciter.xml.retriever.engine.AliasReCiterRetrievalEngine.IdentityNameType;
import reciter.xml.retriever.pubmed.PubMedQueryType.PubMedQueryBuilder;

@Component("goldStandardRetrievalStrategy")
public class GoldStandardRetrievalStrategy extends AbstractRetrievalStrategy {

	private static final String retrievalStrategyName = "GoldStandardRetrievalStrategy";

	/**
	 * Maximum number of PMIDs per chunked PubMed query. Sized to keep the encoded
	 * query string well under PubMed's ~2000-character URL ceiling. PMIDs are 8-digit
	 * comma-separated tokens, so 100 PMIDs ~= 900 chars of payload.
	 */
	static final int PMID_BATCH_SIZE = 100;

	@Autowired
	private IDynamoDbGoldStandardService dynamoDbGoldStandardService;

	@Override
	public String getRetrievalStrategyName() {
		return retrievalStrategyName;
	}

	@Override
	protected List<PubMedQueryType> buildQuery(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames) {
		String uid = requireValidUid(identity);
		List<Long> pmids = loadGoldStandardPmids(uid, true);
		return buildChunkedQueries(pmids, null, null);
	}

	@Override
	protected List<PubMedQueryType> buildQuery(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames,
			Date startDate, Date endDate) {
		String uid = requireValidUid(identity);
		List<Long> pmids = loadGoldStandardPmids(uid, true);
		return buildChunkedQueries(pmids, startDate, endDate);
	}

	public List<PubMedQueryType> buildQueryGoldStandard(Identity identity, Set<Long> uniquePmids) {
		String uid = requireValidUid(identity);
		// Include BOTH knownPmids and rejectedPmids — the engine's analysis metric
		// `inGoldStandardButNotRetrieved` counts any GS PMID (known OR rejected) that
		// wasn't retrieved, so excluding rejected from the dedup variant causes a
		// regression in the metric for users with non-trivial rejectedPmids
		// counts (e.g., rsb2005: 156 rejected → +132 missing in dev smoke retest).
		// Dedup is about avoiding redundant eutils traffic for PMIDs already
		// retrieved by other strategies, not about which GS subset to query.
		List<Long> pmids = loadGoldStandardPmids(uid, true);
		if (uniquePmids != null && !uniquePmids.isEmpty()) {
			pmids.removeAll(uniquePmids);
		}
		return buildChunkedQueries(pmids, null, null);
	}

	/**
	 * Retrieve PubMed articles for an explicit list of pre-built queries.
	 * Used by {@code AliasReCiterRetrievalEngine} after it builds chunked GoldStandard
	 * queries via {@link #buildQueryGoldStandard(Identity, Set)} so the engine can
	 * dedup against {@code uniquePmids} accumulated by prior strategies (FIX-05).
	 *
	 * <p>Delegates to the parent's protected {@code retrievePubMedArticles} helper —
	 * same threshold gating, lenient/strict logic, and PubMed eutils calls as the
	 * standard retrieval path. Identity names map is empty because GS queries are
	 * PMID-list based and do not consult name evidence.
	 */
	public RetrievalResult retrievePubMedArticlesUsingQueries(Identity identity,
			List<PubMedQueryType> pubMedQueries, boolean useStrictQueryOnly) throws IOException {
		return retrievePubMedArticles(identity, Collections.emptyMap(), pubMedQueries, useStrictQueryOnly);
	}

	/**
	 * Build one {@link PubMedQueryType} per chunk of up to {@link #PMID_BATCH_SIZE} PMIDs.
	 *
	 * <p>Count queries are ALWAYS set (date-less, even when the term query carries a date
	 * range) — required because {@code AbstractRetrievalStrategy.retrievePubMedArticles}
	 * unconditionally reads {@code getLenientCountQuery().getQuery()} on every emitted
	 * {@link PubMedQueryType} and would NPE otherwise. Date-restricting the count query
	 * is intentionally avoided so the threshold gate sees the full author result count,
	 * not a date-restricted subset (see PR-spec-goldstandard-chunking.md line 232).
	 *
	 * @param pmids     the PMIDs to encode (chunked); may be null/empty (returns an empty list)
	 * @param startDate optional date-range start applied to TERM queries only; null means no date-range
	 * @param endDate   optional date-range end applied to TERM queries only
	 */
	private List<PubMedQueryType> buildChunkedQueries(List<Long> pmids, Date startDate, Date endDate) {
		List<PubMedQueryType> queries = new ArrayList<>();
		if (pmids == null || pmids.isEmpty()) {
			return queries;
		}

		boolean withDateRange = (startDate != null && endDate != null);
		for (int i = 0; i < pmids.size(); i += PMID_BATCH_SIZE) {
			List<Long> chunk = pmids.subList(i, Math.min(i + PMID_BATCH_SIZE, pmids.size()));

			PubMedQueryBuilder termBuilder = withDateRange
					? new PubMedQueryBuilder().dateRange(true, startDate, endDate)
					: new PubMedQueryBuilder();
			PubMedQuery termQuery = termBuilder.buildPmids(chunk);
			// PubMedQueryBuilder.buildPmids does not propagate startDate/endDate to the
			// returned PubMedQuery; apply them here so PubMedQuery.toString() emits the [DP]/[EDAT] window.
			if (withDateRange) {
				termQuery.setStart(startDate);
				termQuery.setEnd(endDate);
			}

			// Date-less count query always set — see Javadoc for the NPE-avoidance rationale.
			PubMedQuery countQuery = new PubMedQueryBuilder().buildPmids(chunk);

			PubMedQueryType pubMedQueryType = new PubMedQueryType();
			pubMedQueryType.setLenientQuery(new PubMedQueryResult(termQuery));
			pubMedQueryType.setStrictQuery(new PubMedQueryResult(termQuery));
			pubMedQueryType.setLenientCountQuery(new PubMedQueryResult(countQuery));
			pubMedQueryType.setStrictCountQuery(new PubMedQueryResult(countQuery));

			queries.add(pubMedQueryType);
		}
		return queries;
	}

	private String requireValidUid(Identity identity) {
		if (identity == null || identity.getUid() == null || identity.getUid().isBlank()) {
			throw new IllegalArgumentException("Identity UID is missing or blank.");
		}
		return identity.getUid().trim();
	}

	/**
	 * Defensive-copy the GoldStandard PMID list so callers can mutate the result
	 * (e.g. remove unique PMIDs) without polluting the DynamoDB-cached entity.
	 *
	 * @param uid               the resolved (trimmed) identity UID
	 * @param includeRejected   when true, append rejectedPmids to the returned list
	 *                          (the no-args and dateRange overloads target the full GS;
	 *                          the dedup overload targets known PMIDs only).
	 */
	private List<Long> loadGoldStandardPmids(String uid, boolean includeRejected) {
		List<Long> pmids = new ArrayList<>();
		GoldStandard goldStandard = dynamoDbGoldStandardService.findByUid(uid);
		if (goldStandard == null) {
			return pmids;
		}
		if (goldStandard.getKnownPmids() != null) {
			pmids.addAll(goldStandard.getKnownPmids());
		}
		if (includeRejected && goldStandard.getRejectedPmids() != null) {
			pmids.addAll(goldStandard.getRejectedPmids());
		}
		return pmids;
	}

	@Override
	public RetrievalResult retrievePubMedArticles(List<Long> pmids) throws IOException {
		throw new UnsupportedOperationException("Does not support retrieval by pmids.");
	}
}
