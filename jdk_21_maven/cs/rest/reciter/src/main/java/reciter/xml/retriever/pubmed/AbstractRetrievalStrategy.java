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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.model.pubmed.PubMedArticle;
import reciter.model.scopus.ScopusArticle;
import reciter.pubmed.retriever.PubMedArticleRetriever;
import reciter.pubmed.retriever.PubMedQuery;
import reciter.scopus.retriever.ScopusArticleRetriever;
import reciter.xml.retriever.engine.AliasReCiterRetrievalEngine.IdentityNameType;

@Configurable
public abstract class AbstractRetrievalStrategy implements RetrievalStrategy {

	private static final String nodeUrlBegin = "https://reciter-pubmed-retrieval-";
	private static final String nodeUrlEnd = ".herokuapp.com/reciter/retrieve/pubmed/by/query?";
	private static final int nodeSize = 7;

	private static final String scopusNodeUrlBegin = "https://reciter-scopus-retrieval-";
	private static final String scopusNodeUrlEnd = ".herokuapp.com/reciter/retrieve/scopus/by/pmids/";
	private static final int scopusNodeSize = 3;

	public static class RetrievalResult {
		private final Map<Long, PubMedArticle> pubMedArticles;
		private final List<PubMedQueryResult> pubMedQueryResults;
		public RetrievalResult(Map<Long, PubMedArticle> pubMedArticles, List<PubMedQueryResult> pubMedQueryResults) {
			this.pubMedArticles = pubMedArticles;
			this.pubMedQueryResults = pubMedQueryResults;
		}
		public Map<Long, PubMedArticle> getPubMedArticles() {
			return pubMedArticles;
		}
		public List<PubMedQueryResult> getPubMedQueryResults() {
			return pubMedQueryResults;
		}
	}

	private final static Logger slf4jLogger = LoggerFactory.getLogger(AbstractRetrievalStrategy.class);

	/**
	 * Retrieval threshold.
	 */
	@Value("${searchStrategy-lenient-threshold}")
	protected final int DEFAULT_THRESHOLD = 2000;
	
	/**
	 * Strict Retrieval threshold.
	 */
	@Value("${searchStrategy-strict-threshold}")
	protected final int STRICT_THRESHOLD = 1000;

	/**
	 * Scopus retrieval threshold.
	 */
	protected static final int SCOPUS_DEFAULT_THRESHOLD = 24;

	/**
	 * Scopus retrieval max threshold.
	 */
	protected static final int SCOPUS_MAX_THRESHOLD = 25;

	/**
	 * Should retrieved if threshold exceeds.
	 */
	protected boolean isRetrieveExceedThreshold;

	public void setRetrieveExceedThreshold(boolean isRetrieveExceedThreshold) {
		this.isRetrieveExceedThreshold = isRetrieveExceedThreshold;
	}

	protected abstract List<PubMedQueryType> buildQuery(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames);
	protected abstract List<PubMedQueryType> buildQuery(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames, Date startDate, Date endDate);

	@Override
	public RetrievalResult retrievePubMedArticles(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames, Date startDate, Date endDate, boolean useStrictQueryOnly) throws IOException {
		List<PubMedQueryType> pubMedQueries = buildQuery(identity, identityNames, startDate, endDate);
		return retrievePubMedArticles(identity, identityNames, pubMedQueries, useStrictQueryOnly);
	}

	@Override
	public RetrievalResult retrievePubMedArticles(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames, boolean useStrictQueryOnly) throws IOException {
		List<PubMedQueryType> pubMedQueries = buildQuery(identity, identityNames);
		for (PubMedQueryType pubMedQueryType : pubMedQueries) {
			slf4jLogger.info(pubMedQueryType.toString());
		}
		return retrievePubMedArticles(identity, identityNames, pubMedQueries, useStrictQueryOnly);
	}
	
	/*public RetrievalResult retrievePubMedArticlesUsingGoldStandard(Identity identity, Set<Long> uniquePmids) throws IOException {
		List<PubMedQueryType> pubMedQueries = goldStandardRetrievalStrategy.buildQueryGoldStandard(identity, uniquePmids);
		for (PubMedQueryType pubMedQueryType : pubMedQueries) {
			slf4jLogger.info(pubMedQueryType.toString());
		}
		return retrievePubMedArticles(identity, pubMedQueries);
	}*/

	protected RetrievalResult retrievePubMedArticles(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames, List<PubMedQueryType> pubMedQueries, boolean useStrictQueryOnly) throws IOException {

		Map<Long, PubMedArticle> pubMedArticles = new HashMap<Long, PubMedArticle>();

		slf4jLogger.info("Query size: " + pubMedQueries.size());
		List<PubMedQueryResult> pubMedQueryResults = new ArrayList<PubMedQueryResult>();
		int handler = 0;
		for (PubMedQueryType pubMedQueryType : pubMedQueries) {
			
			PubMedQuery encodedInitialCountQuery = pubMedQueryType.getLenientCountQuery().getQuery();
			String countQueryStr = encodedInitialCountQuery == null ? "" : encodedInitialCountQuery.toString().trim();
			if (countQueryStr.isBlank() || countQueryStr.equals("()")) {
				slf4jLogger.info("Skipping degenerate count query [{}] for strategy [{}]", countQueryStr, getRetrievalStrategyName());
				handler = 0;
			} else {
				handler = getNumberOfResults(encodedInitialCountQuery);
			}
			if(!useStrictQueryOnly) {
				slf4jLogger.info("Constructed lenient count query {}", pubMedQueryType.getLenientCountQuery().getQuery());
				slf4jLogger.info("Constructed lenient query {}", pubMedQueryType.getLenientQuery().getQuery());
				//PubMedQuery encodedInitialQuery = pubMedQueryType.getLenientQuery().getQuery();
				//PubMedQuery encodedInitialCountQuery = pubMedQueryType.getLenientCountQuery().getQuery();
				//handler = getNumberOfResults(encodedInitialCountQuery);
				// check number of PubMed results returned by initial query.
				// If it's greater than the threshold, query using the strict query.
				pubMedQueryType.getLenientQuery().setNumResult(handler);
				if (handler > DEFAULT_THRESHOLD) {
					PubMedQuery constructedStrictCountQuery = pubMedQueryType.getStrictCountQuery().getQuery();
					slf4jLogger.info("Constructed strict count query {}", constructedStrictCountQuery);
					slf4jLogger.info("Constructed strict query {}", pubMedQueryType.getStrictQuery().getQuery());
	//				String strictQuery = URLEncoder.encode(constructedStrictQuery, "UTF-8");
					int strictSearchHandler = getNumberOfResults(constructedStrictCountQuery);
					slf4jLogger.info("strictSearchHandler : {}", strictSearchHandler);
					pubMedQueryType.getStrictQuery().setNumResult(strictSearchHandler);
	
					// only retrieve articles if number is less than threshold, otherwise the article download
					// may take too long
					if (strictSearchHandler <= STRICT_THRESHOLD) {
						List<PubMedArticle> result = retrievePubMed(pubMedQueryType.getStrictQuery().getQuery(), strictSearchHandler);
						for (PubMedArticle pubMedArticle : result) {
							long pmid = pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid();
							if (!pubMedArticles.containsKey(pmid)) {
								pubMedArticles.put(pmid, pubMedArticle);
							}
						}
						pubMedQueryType.getStrictQuery().setUsed(true);
					}
				} else if(handler > 0) {
					List<PubMedArticle> result = retrievePubMed(pubMedQueryType.getLenientQuery().getQuery(), handler);
					for (PubMedArticle pubMedArticle : result) {
						long pmid = pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid();
						if (!pubMedArticles.containsKey(pmid)) {
							pubMedArticles.put(pmid, pubMedArticle);
						}
					}
					pubMedQueryType.getLenientQuery().setUsed(true);
				}
			} else if(handler > 0){ // added condition to prevent executing strict strategy query for the first Name initial when lenient query returns 0 records.
				PubMedQuery constructedStrictCountQuery = pubMedQueryType.getStrictCountQuery().getQuery();
				slf4jLogger.info("Constructed strict count query {}", constructedStrictCountQuery);
				slf4jLogger.info("Constructed strict query {}", pubMedQueryType.getStrictQuery().getQuery());
	
//				String strictQuery = URLEncoder.encode(constructedStrictQuery, "UTF-8");
				int strictSearchHandler = getNumberOfResults(constructedStrictCountQuery);
				slf4jLogger.info("strictSearchHandler in else if handler >0 :{}", strictSearchHandler);
				pubMedQueryType.getStrictQuery().setNumResult(strictSearchHandler);

				// only retrieve articles if number is less than threshold, otherwise the article download
				// may take too long
				if (strictSearchHandler <= STRICT_THRESHOLD) {
					List<PubMedArticle> result = retrievePubMed(pubMedQueryType.getStrictQuery().getQuery(), strictSearchHandler);
					for (PubMedArticle pubMedArticle : result) {
						long pmid = pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid();
						if (!pubMedArticles.containsKey(pmid)) {
							pubMedArticles.put(pmid, pubMedArticle);
						}
					}
					pubMedQueryType.getStrictQuery().setUsed(true);
				}
			}

			pubMedQueryResults.add(pubMedQueryType.getLenientQuery());
			pubMedQueryResults.add(pubMedQueryType.getStrictQuery());
		}
		slf4jLogger.info("Found " + pubMedArticles.size() + " PubMed articles for " + identity.getUid() 
		+ " using retrieval strategy [" + getRetrievalStrategyName() + "]");
		
		slf4jLogger.info("pubMedQueryResults : {}", pubMedQueryResults.size());

		return new RetrievalResult(pubMedArticles, pubMedQueryResults);
	}

	/**
	 * Randomly select a node.
	 * @return
	 */
	private String loadBalance() {
		int nodeSelected = (int) (Math.random() * nodeSize + 1);
		return nodeUrlBegin + nodeSelected + nodeUrlEnd;
	}

	private String loadBalanceScopus() {
		int nodeSelected = (int) (Math.random() * scopusNodeSize + 1);
		return scopusNodeUrlBegin + nodeSelected + scopusNodeUrlEnd;
	}

	private class Response<T> {
		private final boolean shouldRetrieve;
		private final T response;

		public Response(boolean shouldRetrieve, T response) {
			this.shouldRetrieve = shouldRetrieve;
			this.response = response;
		}
		public boolean isShouldRetrieve() {
			return shouldRetrieve;
		}
		public T getResponse() {
			return response;
		}
	}

	public List<PubMedArticle> retrievePubMed(PubMedQuery pubMedQuery, int numberOfPubmedArticles)  {
		PubMedArticleRetriever pubMedArticleRetriever = new PubMedArticleRetriever();
		return pubMedArticleRetriever.retrievePubMed(pubMedQuery, numberOfPubmedArticles);
	}

	@Override
	public List<ScopusArticle> retrieveScopus(Collection<Long> pmids) {
		ScopusArticleRetriever<Long> scopusArticleRetriever = new ScopusArticleRetriever<Long>();
		return scopusArticleRetriever.retrieveScopus(ScopusArticleRetriever.PMID_MODIFIER, new ArrayList<Long>(pmids));
	}
	
	@Override
	public List<ScopusArticle> retrieveScopusDoi(Collection<String> dois) {
		ScopusArticleRetriever<String> scopusArticleRetriever = new ScopusArticleRetriever<String>();
		return scopusArticleRetriever.retrieveScopus(ScopusArticleRetriever.DOI_MODIFIER, new ArrayList<String>(dois));
	}

	private static final String PUBMED_SERVICE = System.getenv("PUBMED_SERVICE");

	protected int getNumberOfResults(PubMedQuery pubMedQueryType) throws IOException {
		String nodeUrl = PUBMED_SERVICE.endsWith("/pubmed")?PUBMED_SERVICE + "/query-number-pubmed-articles/":PUBMED_SERVICE + "/pubmed/query-number-pubmed-articles/";
		
		RestTemplate restTemplate = new RestTemplate();
		slf4jLogger.info("Sending web request: " + nodeUrl);
		ResponseEntity<Integer> responseEntity = null;
		slf4jLogger.info("PubMedQuery: " + pubMedQueryType);
		try {
			responseEntity = restTemplate.postForEntity(nodeUrl, pubMedQueryType, Integer.class);
		} catch (Exception e) {
			slf4jLogger.error("Unable to retrieve via external REST api=[" + nodeUrl + "]", e);
		}
		if (responseEntity == null) {
			// Count query failed (tool 500 / NCBI error). Flag the run so the engine rolls the
			// retrievalDate watermark back instead of treating this as a legitimate "0 results". #689
			RetrievalErrorTracker.markError();
			return 0;
		}
		int results = responseEntity.getBody();
		slf4jLogger.info("Returned results for query:" + pubMedQueryType + ":" + results);
		return results;
	}

	private List<PubMedArticle> retrievePubMedViaRest(String pubMedQuery) {
		String nodeUrl = loadBalance();
		RestTemplate restTemplate = new RestTemplate();
		slf4jLogger.info("Sending web request: " + nodeUrl);
		ResponseEntity<PubMedArticle[]> responseEntity = null;
		try {
			responseEntity = restTemplate.getForEntity(nodeUrl, PubMedArticle[].class);
		} catch (Exception e) {
			slf4jLogger.error("Unable to retrieve via external REST api=[" + nodeUrl + "]", e);
		}
		PubMedArticle[] pubMedArticles = responseEntity.getBody();
		return Arrays.asList(pubMedArticles);
	}

	private Response<List<ScopusArticle>> retrieveScopusViaRest(Collection<Long> pmids) {
		if (pmids.isEmpty()) {
			return new Response<List<ScopusArticle>>(false, null);
		}
		String nodeUrl = loadBalanceScopus() + "?pmids=";
		StringBuffer sb = new StringBuffer();
		List<String> pmidQueries = new ArrayList<String>();
		int i = 0;
		Iterator<Long> itr = pmids.iterator();
		while (itr.hasNext()) {
			long pmid = itr.next();
			if (i == 0 || (i % SCOPUS_DEFAULT_THRESHOLD != 0 && i != pmids.size() - 1)) {
				sb.append(pmid);
				sb.append(",");
			} else {
				sb.append(pmid);
			}
			if (i != 0 && i % SCOPUS_DEFAULT_THRESHOLD == 0) {
				pmidQueries.add(sb.toString());
				sb = new StringBuffer();
			}
			i++;
		}
		// add the remaining pmids
		String remaining = sb.toString();
		if (!remaining.isBlank()) {
			pmidQueries.add(remaining);
		}
		List<ScopusArticle> scopusArticlesResult = new ArrayList<ScopusArticle>();
		for (String pmidQuery : pmidQueries) {
			RestTemplate restTemplate = new RestTemplate();
			try {
				slf4jLogger.info("Sending web request: " + nodeUrl + pmidQuery);
				ResponseEntity<ScopusArticle[]> responseEntity = restTemplate.getForEntity(nodeUrl + pmidQuery, ScopusArticle[].class);
				ScopusArticle[] scopusArticles = responseEntity.getBody();
				scopusArticlesResult.addAll(Arrays.asList(scopusArticles));
				slf4jLogger.info("Retrieved scopusArticles size=[" + scopusArticles.length + "]");
			} catch (Exception e) {
				slf4jLogger.error("Unable to retrieve via external REST api=[" + nodeUrl + pmidQuery + "]", e);
				return new Response<List<ScopusArticle>>(true, null);
			}
		}
		return new Response<List<ScopusArticle>>(false, scopusArticlesResult);
	}
}
