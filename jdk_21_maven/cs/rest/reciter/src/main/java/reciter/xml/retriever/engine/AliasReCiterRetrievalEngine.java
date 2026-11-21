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
package reciter.xml.retriever.engine;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reciter.algorithm.evidence.targetauthor.TargetAuthorSelection;
import reciter.algorithm.util.ArticleTranslator;
import reciter.api.parameters.RetrievalRefreshFlag;
import reciter.database.dynamodb.model.ESearchCount;
import reciter.database.dynamodb.model.GoldStandard;
import reciter.database.dynamodb.model.PmidProvenance;
import reciter.database.dynamodb.model.QueryType;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.AuthorName;
import reciter.model.identity.Identity;
import reciter.model.pubmed.PubMedArticle;
import reciter.model.scopus.ScopusArticle;
import reciter.service.ArticleProvenanceService;
import reciter.service.ESearchCountService;
import reciter.service.ESearchResultService;
import reciter.service.PmidProvenanceService;
import reciter.service.dynamo.IDynamoDbGoldStandardService;
import reciter.utils.AuthorNameSanitizationUtils;
import reciter.utils.ReCiterStringUtil;
import reciter.xml.retriever.pubmed.AbstractRetrievalStrategy.RetrievalResult;
import reciter.xml.retriever.pubmed.PubMedQueryType;
import reciter.xml.retriever.pubmed.RetrievalErrorTracker;

@Component("aliasReCiterRetrievalEngine")
public class AliasReCiterRetrievalEngine extends AbstractReCiterRetrievalEngine {

	private final static Logger slf4jLogger = LoggerFactory.getLogger(AliasReCiterRetrievalEngine.class);

	@Value("${use.scopus.articles}")
	private boolean useScopusArticles;
	
	@Value("${searchStrategy-lenient-threshold}")
	private double searchStrategyLenientThreshold;
	
	@Autowired
	private IDynamoDbGoldStandardService dynamoDbGoldStandardService;
	
	@Autowired
	private ESearchResultService eSearchResultService;

	@Autowired
	private ESearchCountService eSearchCountService;

	@Autowired
	private PmidProvenanceService pmidProvenanceService;

	@Autowired
	private ArticleProvenanceService articleProvenanceService;

	public enum IdentityNameType {
		ORIGINAL,
		DERIVED
	}
	
	private class AsyncRetrievalEngine extends Thread {

		private final Identity identity;
		private final Date startDate;
		private final Date endDate;
		private final RetrievalRefreshFlag refreshFlag;
		private final Set<String> failedUids;

		public AsyncRetrievalEngine(Identity identity, Date startDate, Date endDate, RetrievalRefreshFlag refreshFlag, Set<String> failedUids) {
			this.identity = identity;
			this.startDate = startDate;
			this.endDate = endDate;
			this.refreshFlag = refreshFlag;
			this.failedUids = failedUids;
		}

		@Override
		public void run() {
			try {
				// If the eSearchResult collection doesn't contain any information regarding this person,
				// then we'd want to perform a full retrieval because this will be first time that ReCiter
				// retrieve PubMed and Scopus articles for this person.
				slf4jLogger.info("this.refreshFlag in Alias run" + this.refreshFlag);
				if(this.refreshFlag == RetrievalRefreshFlag.ALL_PUBLICATIONS) {
					slf4jLogger.info("Starting full retrieval for uid=[" + identity.getUid() + "].");
					retrieveData(identity, this.refreshFlag);
					// The strategies SWALLOW a PubMed tool 500 / NCBI 429: AbstractRetrievalStrategy
					// and PubMedArticleRetriever catch, mark the thread-local tracker, and return
					// empty rather than throwing. Nothing reaches the catch below, so a tool-wide
					// outage looks to the gate like a clean run that simply found nothing. On THIS
					// path retrieveArticlesByUid has already deleted the prior ESearchResult, so
					// scoring that empty candidate set overwrites the Analysis row with zero
					// features and answers 200 — the May 3-4 shape (#720). Record it as a failure.
					// The incremental path is protected differently: the engine's per-strategy
					// union when a run hit failures (#737, upsertedStrategyEntry) stops a
					// partial batch from shrinking the stored ESearchResult entry, and #691's
					// watermark rollback keeps the next incremental run from treating the gap
					// as already covered.
					if (RetrievalErrorTracker.hadError()) {
						slf4jLogger.error("Retrieval for uid=[" + identity.getUid()
								+ "] finished with swallowed PubMed failures; treating the run as failed.");
						failedUids.add(identity.getUid());
					}
				} else if(this.refreshFlag == RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS) {
					slf4jLogger.info("Starting date range retrieval for uid=[" + identity.getUid() + "] startDate=["
						+ startDate + "] endDate=[" + endDate + "].");
					retrieveDataByDateRange(identity, startDate, endDate, this.refreshFlag);
				}
			} catch (Throwable t) {
				// ForkJoinPool.execute() stores a task's throwable without ever rethrowing it to
				// an uncaught-exception handler, so an unrecorded crash here is invisible and the
				// caller would score an empty candidate set as if retrieval had succeeded. Catch
				// Throwable, not Exception: an Error (OOM, StackOverflowError, NoClassDefFoundError)
				// must also mark this uid as failed, or the false-return gate never fires. Record
				// and log FIRST, then rethrow Errors to preserve JVM Error semantics — the pool
				// swallows the rethrow, but the uid is already recorded.
				slf4jLogger.error("Unabled to retrieve. " + identity.getUid(), t);
				failedUids.add(identity.getUid());
				if (t instanceof Error) {
					throw (Error) t;
				}
			}
		}
	}

	@Override
	public boolean retrieveArticlesByDateRange(List<Identity> identities, Date startDate, Date endDate, RetrievalRefreshFlag refreshFlag) throws IOException {
		ExecutorService executorService = Executors.newWorkStealingPool(15);//Executors.newFixedThreadPool(10);
		Set<String> failedUids = ConcurrentHashMap.newKeySet();
		for (Identity identity : identities) {
			executorService.execute(new AsyncRetrievalEngine(identity, startDate, endDate, refreshFlag, failedUids));
		}
		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			slf4jLogger.error("Thread interrupted while waiting for retrieval to finish.");
			return false;
		}
		if (!failedUids.isEmpty()) {
			slf4jLogger.error("Retrieval crashed for uids=" + failedUids + ".");
			return false;
		}
		return true;
	}
	
	/** Package-private, not private, so tests can override it to simulate a worker Error
	 *  or a swallowed strategy failure. */
	Set<Long> retrieveData(Identity identity, RetrievalRefreshFlag refreshFlag) throws IOException {
		slf4jLogger.info("Coming into retrieveData section without date range****");
		Set<Long> uniquePmids = new HashSet<>();

		QueryType queryType = null;

		//eSearchResultService.delete();

		String uid = identity.getUid();

		// Clean-completion tracking (#696): clear the per-run PubMed-failure flag so the
		// lastFullSweep stamp at the end of this method reflects THIS run only. This is the
		// RetrievalErrorTracker discipline #691 established for retrieveDataByDateRange,
		// extended to the full-sweep path — which is the path both manual and escalated
		// ALL_PUBLICATIONS requests take.
		RetrievalErrorTracker.reset();

		// Phase 1 provenance tracking state
		Set<Long> nonGsStrategyPmids = new HashSet<>();
		Map<Long, String> newPmidStrategy = new LinkedHashMap<>();
		Set<Long> backfillPmids = new HashSet<>(pmidProvenanceService.findPmidsByUidAndStrategy(uid, "BACKFILL_FROM_ESEARCHRESULT"));
		// Build set of already-known PMIDs from existing ESearchResult
		Set<Long> existingPmids = new HashSet<>();
		reciter.database.dynamodb.model.ESearchResult existingESearch = eSearchResultService.findByUid(uid);
		if (existingESearch != null && existingESearch.getESearchPmids() != null) {
			for (reciter.database.dynamodb.model.ESearchPmid esp : existingESearch.getESearchPmids()) {
				if (esp.getPmids() != null) {
					existingPmids.addAll(esp.getPmids());
				}
			}
		}

		Map<IdentityNameType, Set<AuthorName>> identityNames = new LinkedHashMap<IdentityNameType, Set<AuthorName>>();
		identityAuthorNames(identity, identityNames);
		boolean useStrictQueryOnly = identityNames.entrySet().stream().anyMatch(entry -> entry.getKey() == IdentityNameType.DERIVED && entry.getValue().size() > 0);

		if(useStrictQueryOnly) {
			queryType = QueryType.STRICT_COMPOUND_NAME_LOOKUP;
		}

		// Initialize as an empty map up front so all strategies use putAll uniformly.
		// (Phase 36 FIX-05) GoldStandard retrieval now runs LAST after Email/FNI/Aff/Dept/Grant/etc.
		// have populated uniquePmids, so the GS dedup filter has full coverage. ORCID inference
		// and the ORCID retrieval strategy also move to after GS-last so they still see GS-retrieved
		// articles in pubMedArticles when inferOrcidFromAcceptedArticles runs.
		Map<Long, PubMedArticle> pubMedArticles = new HashMap<>();
		GoldStandard goldStandard = dynamoDbGoldStandardService.findByUid(identity.getUid().trim());

		// Retrieve by email.
		RetrievalResult retrievalResult = emailRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
		pubMedArticles.putAll(retrievalResult.getPubMedArticles());
		slf4jLogger.info("pubMedArticles in retrieveData section without date range****"+pubMedArticles.size());
		/*if (pubMedArticles.size() > 0) {
			Map<Long, AuthorName> aliasSet = AuthorNameUtils.calculatePotentialAlias(identity, pubMedArticles.values());

			slf4jLogger.info("Found " + aliasSet.size() + " new alias for uid=[" + uid + "]");

			// Update alias.
			List<PubMedAlias> pubMedAliases = new ArrayList<>();
			for (Map.Entry<Long, AuthorName> entry : aliasSet.entrySet()) {
				PubMedAlias pubMedAlias = new PubMedAlias();
				pubMedAlias.setAuthorName(entry.getValue());
				pubMedAlias.setPmid(entry.getKey());
				slf4jLogger.info("new alias for uid=[" + identity.getUid() + "], alias=[" + entry.getValue() + "] from pmid=[" + entry.getKey() + "]");
				pubMedAliases.add(pubMedAlias);
			}

			identity.setPubMedAlias(pubMedAliases);
			Date date = new Date();
			identity.setDateInitialRun(date);
			identity.setDateLastRun(date);
			identityService.save(identity);

			uniquePmids.addAll(pubMedArticles.keySet());
		}*/

		// TODO parallelize by putting save in a separate thread.
		savePubMedArticles(pubMedArticles.values(), uid, emailRetrievalStrategy.getRetrievalStrategyName(), retrievalResult.getPubMedQueryResults(), queryType, refreshFlag);
		uniquePmids.addAll(pubMedArticles.keySet());
		nonGsStrategyPmids.addAll(pubMedArticles.keySet());
		trackNewPmids(pubMedArticles, emailRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);
		
		RetrievalResult r1;
		if(useStrictQueryOnly) {
			r1 = firstNameInitialRetrievalStrategy.retrievePubMedArticles(identity, identityNames, false);
		} else {
			r1 = firstNameInitialRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
		}
		//if (r1.getPubMedArticles().size() > 0) {
		if(r1.getPubMedQueryResults() != null
				&&
				r1.getPubMedQueryResults().size() > 0
				&&
				r1.getPubMedQueryResults().get(0).getNumResult() < searchStrategyLenientThreshold) {
			if(queryType == null) {
				queryType = QueryType.LENIENT_LOOKUP;
			}
			pubMedArticles.putAll(r1.getPubMedArticles());
			savePubMedArticles(r1.getPubMedArticles().values(), uid, firstNameInitialRetrievalStrategy.getRetrievalStrategyName(), r1.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r1.getPubMedArticles().keySet());
			nonGsStrategyPmids.addAll(r1.getPubMedArticles().keySet());
			trackNewPmids(r1.getPubMedArticles(), firstNameInitialRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);
		}
		//toggle useStrictQUery as true if results from Last Name First Initial Strategy is larger than lenientStrategy
		if(r1.getPubMedQueryResults().get(0).getNumResult() > searchStrategyLenientThreshold) {
			useStrictQueryOnly = true;
			queryType = QueryType.STRICT_EXCEEDS_THRESHOLD_LOOKUP;

			// Store the true eSearch count so ArticleSizeStrategy can use log(count) for scoring.
			// This avoids a separate live eSearch call during the scoring phase.
			int trueCount = r1.getPubMedQueryResults().get(0).getNumResult();
			eSearchCountService.save(new ESearchCount(uid, trueCount));
			slf4jLogger.info("Stored eSearchCount={} for uid={}", trueCount, uid);
		}

		if(r1.getPubMedQueryResults() != null
				&&
				r1.getPubMedQueryResults().size() > 0
				&&
				r1.getPubMedQueryResults().get(0).getNumResult() > searchStrategyLenientThreshold
				||
				useStrictQueryOnly) {
			//Check to see if there is an actual need to do query for all steps
			if(identity.getInstitutions() != null && !identity.getInstitutions().isEmpty()) {
				RetrievalResult r2 = affiliationInDbRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
				pubMedArticles.putAll(r2.getPubMedArticles());
				savePubMedArticles(r2.getPubMedArticles().values(), uid, affiliationInDbRetrievalStrategy.getRetrievalStrategyName(), r2.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r2.getPubMedArticles().keySet());
				nonGsStrategyPmids.addAll(r2.getPubMedArticles().keySet());
				trackNewPmids(r2.getPubMedArticles(), affiliationInDbRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			} else {
				slf4jLogger.info("Skipping " + affiliationInDbRetrievalStrategy.getRetrievalStrategyName() + " since no affiliation for " + identity.getUid());
			}

			RetrievalResult r3 = affiliationRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
			pubMedArticles.putAll(r3.getPubMedArticles());
			savePubMedArticles(r3.getPubMedArticles().values(), uid, affiliationRetrievalStrategy.getRetrievalStrategyName(), r3.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r3.getPubMedArticles().keySet());
			nonGsStrategyPmids.addAll(r3.getPubMedArticles().keySet());
			trackNewPmids(r3.getPubMedArticles(), affiliationRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			if(identity.getOrganizationalUnits() != null && !identity.getOrganizationalUnits().isEmpty()) {
				RetrievalResult r4 = departmentRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
				pubMedArticles.putAll(r4.getPubMedArticles());
				savePubMedArticles(r4.getPubMedArticles().values(), uid, departmentRetrievalStrategy.getRetrievalStrategyName(), r4.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r4.getPubMedArticles().keySet());
				nonGsStrategyPmids.addAll(r4.getPubMedArticles().keySet());
				trackNewPmids(r4.getPubMedArticles(), departmentRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			} else {
				slf4jLogger.info("Skipping " + departmentRetrievalStrategy.getRetrievalStrategyName() + " since no departments for " + identity.getUid());
			}

			if(identity.getGrants() != null && !identity.getGrants().isEmpty()) {
				RetrievalResult r5 = grantRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
				pubMedArticles.putAll(r5.getPubMedArticles());
				savePubMedArticles(r5.getPubMedArticles().values(), uid, grantRetrievalStrategy.getRetrievalStrategyName(), r5.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r5.getPubMedArticles().keySet());
				nonGsStrategyPmids.addAll(r5.getPubMedArticles().keySet());
				trackNewPmids(r5.getPubMedArticles(), grantRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			} else {
				slf4jLogger.info("Skipping " + grantRetrievalStrategy.getRetrievalStrategyName() + " since no grants for " + identity.getUid());
			}

			RetrievalResult r6 = fullNameRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
			pubMedArticles.putAll(r6.getPubMedArticles());
			savePubMedArticles(r6.getPubMedArticles().values(), uid, fullNameRetrievalStrategy.getRetrievalStrategyName(), r6.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r6.getPubMedArticles().keySet());
			nonGsStrategyPmids.addAll(r6.getPubMedArticles().keySet());
			trackNewPmids(r6.getPubMedArticles(), fullNameRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			if(identity.getKnownRelationships() != null && !identity.getKnownRelationships().isEmpty()) {
				RetrievalResult r7 = knownRelationshipRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
				pubMedArticles.putAll(r7.getPubMedArticles());
				savePubMedArticles(r7.getPubMedArticles().values(), uid, knownRelationshipRetrievalStrategy.getRetrievalStrategyName(), r7.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r7.getPubMedArticles().keySet());
				nonGsStrategyPmids.addAll(r7.getPubMedArticles().keySet());
				trackNewPmids(r7.getPubMedArticles(), knownRelationshipRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			} else {
				slf4jLogger.info("Skipping " + knownRelationshipRetrievalStrategy.getRetrievalStrategyName() + " since no Known Relationships for " + identity.getUid());
			}

			RetrievalResult r8 = secondIntialRetrievalStrategy.retrievePubMedArticles(identity, identityNames, useStrictQueryOnly);
			pubMedArticles.putAll(r8.getPubMedArticles());
			savePubMedArticles(r8.getPubMedArticles().values(), uid, secondIntialRetrievalStrategy.getRetrievalStrategyName(), r8.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r8.getPubMedArticles().keySet());
			nonGsStrategyPmids.addAll(r8.getPubMedArticles().keySet());
			trackNewPmids(r8.getPubMedArticles(), secondIntialRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

		}

		// Phase 36 FIX-05: GoldStandard retrieval runs LAST so it can dedup against the
		// uniquePmids accumulated by every prior name/email/affiliation/grant strategy.
		// The chunked GS query builder from Plan 36-01 batches PMIDs in groups of 100;
		// the dedup filter strips any knownPmid already retrieved by another strategy.
		// Net effect: zero redundant eutils traffic for PMIDs already in hand.
		List<PubMedQueryType> gsQueries = goldStandardRetrievalStrategy.buildQueryGoldStandard(identity, uniquePmids);
		RetrievalResult goldStandardRetrievalResult =
				goldStandardRetrievalStrategy.retrievePubMedArticlesUsingQueries(identity, gsQueries, useStrictQueryOnly);
		pubMedArticles.putAll(goldStandardRetrievalResult.getPubMedArticles());
		savePubMedArticles(goldStandardRetrievalResult.getPubMedArticles().values(), uid,
				goldStandardRetrievalStrategy.getRetrievalStrategyName(),
				goldStandardRetrievalResult.getPubMedQueryResults(), queryType, refreshFlag);
		uniquePmids.addAll(goldStandardRetrievalResult.getPubMedArticles().keySet());
		trackNewPmids(goldStandardRetrievalResult.getPubMedArticles(),
				goldStandardRetrievalStrategy.getRetrievalStrategyName(),
				uid, existingPmids, newPmidStrategy, backfillPmids);

		// Retrieve by ORCID (asserted from Identity, or inferred from accepted articles).
		// Inference walks GoldStandard.knownPmids against pubMedArticles, so it must run
		// AFTER GS retrieval has populated those PMIDs. The ORCID strategy then runs
		// against either the asserted or inferred ORCID and adds any new articles.
		String orcidForRetrieval = null;
		if (identity.getOrcid() != null && !identity.getOrcid().isEmpty()
				&& !"NOT SET".equalsIgnoreCase(identity.getOrcid().trim())) {
			orcidForRetrieval = identity.getOrcid().trim();
			slf4jLogger.info("Using asserted ORCID [{}] for uid=[{}]", orcidForRetrieval, uid);
		}
		if (orcidForRetrieval == null) {
			orcidForRetrieval = inferOrcidFromAcceptedArticles(pubMedArticles, goldStandard, identity);
			if (orcidForRetrieval != null) {
				slf4jLogger.info("Inferred ORCID [{}] from accepted articles for uid=[{}]",
						orcidForRetrieval, uid);
				// Set in-memory so OrcidRetrievalStrategy.constructOrcidQuery() can read it.
				// Do NOT persist to DynamoDB — Identity.orcid is for human-asserted ORCIDs only.
				identity.setOrcid(orcidForRetrieval);
				slf4jLogger.info("Using inferred ORCID [{}] (in-memory only) for retrieval, uid=[{}]",
						orcidForRetrieval, uid);
			}
		}
		if (orcidForRetrieval != null) {
			// Strategy reads identity.getOrcid() directly (thread-safe).
			RetrievalResult orcidResult = orcidRetrievalStrategy.retrievePubMedArticles(
					identity, identityNames, useStrictQueryOnly);
			pubMedArticles.putAll(orcidResult.getPubMedArticles());
			savePubMedArticles(orcidResult.getPubMedArticles().values(), uid,
					orcidRetrievalStrategy.getRetrievalStrategyName(),
					orcidResult.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(orcidResult.getPubMedArticles().keySet());
			nonGsStrategyPmids.addAll(orcidResult.getPubMedArticles().keySet());
			trackNewPmids(orcidResult.getPubMedArticles(),
					orcidRetrievalStrategy.getRetrievalStrategyName(),
					uid, existingPmids, newPmidStrategy, backfillPmids);
		}

		// Phase 1: Save ESearchCount for users who didn't already get a threshold-path
		// count (line 275). For threshold-exceeding users, the raw PubMed count is
		// the correct value for ArticleSizeStrategy's log(count) scoring formula.
		// This guard will be removed in Phase 2 when ArticleSizeStrategy is updated
		// to use the retrieved-PMID semantic consistently.
		if (queryType != QueryType.STRICT_EXCEEDS_THRESHOLD_LOOKUP) {
			eSearchCountService.save(new ESearchCount(uid, nonGsStrategyPmids.size()));
			slf4jLogger.info("Stored final eSearchCount={} for uid={}", nonGsStrategyPmids.size(), uid);
		}

		// Phase 1: Write provenance records for newly discovered PMIDs
		if (!newPmidStrategy.isEmpty()) {
			Date now = new Date();
			List<PmidProvenance> provenanceRecords = new ArrayList<>();
			for (Map.Entry<Long, String> entry : newPmidStrategy.entrySet()) {
				provenanceRecords.add(new PmidProvenance(uid, entry.getKey(), Instant.now(), entry.getValue()));
			}
			pmidProvenanceService.saveAllIfNotExists(provenanceRecords);
			slf4jLogger.info("Wrote {} provenance records for uid={}", provenanceRecords.size(), uid);
		}

		if (useScopusArticles) {
			List<ScopusArticle> scopusArticles = emailRetrievalStrategy.retrieveScopus(uniquePmids);

			scopusService.save(scopusArticles);

			// Look up the remaining Scopus articles by DOI.
			List<Long> notFoundPmids = new ArrayList<>();
			Set<Long> foundPmids = new HashSet<>();
			for (ScopusArticle scopusArticle : scopusArticles) {
				foundPmids.add(scopusArticle.getPubmedId());
			}
			// Find the pmids that were not found by using pmid query to Scopus.
			for (long pmid : uniquePmids) {
				if (!foundPmids.contains(pmid)) {
					notFoundPmids.add(pmid);
				}
			}

			slf4jLogger.info("Scopus PMID lookup for uid=[{}]: queried={}, matched={}, notFound={}",
					uid, uniquePmids.size(), foundPmids.size(), notFoundPmids.size());

			List<String> dois = new ArrayList<>();
			Map<String, Long> doiToPmid = new HashMap<>();
			int noDoisCount = 0;
			for (long pmid : notFoundPmids) {
				PubMedArticle pubMedArticle = pubMedArticles.get(pmid);

				if (pubMedArticle != null &&
						pubMedArticle.getMedlinecitation() != null &&
						pubMedArticle.getMedlinecitation().getArticle() != null &&
						pubMedArticle.getMedlinecitation().getArticle().getElocationid() != null &&
						pubMedArticle.getMedlinecitation().getArticle().getElocationid().getElocationid() != null) {
					String doi = pubMedArticle.getMedlinecitation().getArticle().getElocationid().getElocationid().toLowerCase();
					dois.add(doi);
					doiToPmid.put(doi, pmid);
				} else {
					noDoisCount++;
				}
			}

			slf4jLogger.info("Scopus DOI fallback for uid=[{}]: notFoundPmids={}, withDoi={}, withoutDoi={}",
					uid, notFoundPmids.size(), dois.size(), noDoisCount);

			List<ScopusArticle> scopusArticlesByDoi = emailRetrievalStrategy.retrieveScopusDoi(dois);
			List<Long> pmidsByDoi = new ArrayList<>();
			int doiMatchSuccess = 0;
			int doiMatchFailed = 0;
			for (ScopusArticle scopusArticle : scopusArticlesByDoi) {
				// manually insert PMID information.
				if (scopusArticle.getDoi() != null && !scopusArticle.getDoi().isEmpty()) {
					if(doiToPmid.get(scopusArticle.getDoi().toLowerCase()) != null) {
						scopusArticle.setPubmedId(doiToPmid.get(scopusArticle.getDoi().toLowerCase()));
						doiMatchSuccess++;
					} else {
						slf4jLogger.warn("Scopus DOI fallback: DOI mismatch for uid=[{}] — Scopus returned doi=[{}] which has no reverse PMID mapping",
								uid, scopusArticle.getDoi());
						doiMatchFailed++;
					}
				} else {
					doiMatchFailed++;
				}
				pmidsByDoi.add(scopusArticle.getPubmedId());
			}

			slf4jLogger.info("Scopus DOI fallback results for uid=[{}]: doisQueried={}, scopusReturned={}, pmidInjected={}, pmidFailed={}, stillUnmatched={}",
					uid, dois.size(), scopusArticlesByDoi.size(), doiMatchSuccess, doiMatchFailed,
					notFoundPmids.size() - doiMatchSuccess);

			scopusService.save(scopusArticlesByDoi);
		}
		slf4jLogger.info("Finished retrieval for uid=[{}], uniquePmids={}", identity.getUid(), uniquePmids.size());

		// Record the completed full sweep (#696) — only if no strategy entered the
		// error-swallow path during this run. A strategy that threw outright never
		// reaches this line, which is the same outcome: no stamp, person stays due.
		stampLastFullSweepIfClean(uid, refreshFlag);

		return uniquePmids;
	}

	public void retrieveDataByDateRange(Identity identity, Date startDate, Date endDate, RetrievalRefreshFlag refreshFlag) throws IOException {
		slf4jLogger.info("Coming in retrieveData section with date range****");
		Set<Long> uniquePmids = new HashSet<>();
		QueryType queryType = null;
		String uid = identity.getUid();

		// Phase 1 provenance tracking state (no always-save ESearchCount for date-range runs)
		Map<Long, String> newPmidStrategy = new LinkedHashMap<>();
		Set<Long> backfillPmids = new HashSet<>(pmidProvenanceService.findPmidsByUidAndStrategy(uid, "BACKFILL_FROM_ESEARCHRESULT"));
		Set<Long> existingPmids = new HashSet<>();
		reciter.database.dynamodb.model.ESearchResult existingESearch = eSearchResultService.findByUid(uid);
		// Watermark guard (#689): remember where retrievalDate stood before this run and clear the
		// per-run error flag. savePubMedArticles advances retrievalDate to now() as strategies run;
		// if any strategy's PubMed call fails below, we roll it back at the end so the next
		// ONLY_NEWLY_ADDED run re-covers this window instead of skipping past a silently-missed day.
		java.time.Instant preRunRetrievalDate = existingESearch != null ? existingESearch.getRetrievalDate() : null;
		RetrievalErrorTracker.reset();
		try { // body left un-reindented to keep the diff reviewable; the finally rolls the watermark back
		if (existingESearch != null && existingESearch.getESearchPmids() != null) {
			for (reciter.database.dynamodb.model.ESearchPmid esp : existingESearch.getESearchPmids()) {
				if (esp.getPmids() != null) {
					existingPmids.addAll(esp.getPmids());
				}
			}
		}

		Map<IdentityNameType, Set<AuthorName>> identityNames = new LinkedHashMap<IdentityNameType, Set<AuthorName>>();
		identityAuthorNames(identity, identityNames);

		boolean useStrictQueryOnly = identityNames.entrySet().stream().anyMatch(entry -> entry.getKey() == IdentityNameType.DERIVED && entry.getValue().size() > 0);

		if(useStrictQueryOnly) {
			queryType = QueryType.STRICT_COMPOUND_NAME_LOOKUP;
		}

		// (Phase 36 FIX-05) GoldStandard retrieval moved to AFTER the other strategies so it
		// can dedup against uniquePmids. ORCID inference + ORCID-strategy moved with it so
		// inference still sees GS-retrieved articles in pubMedArticles when it runs.
		Map<Long, PubMedArticle> pubMedArticles = new HashMap<>();
		GoldStandard goldStandard = dynamoDbGoldStandardService.findByUid(identity.getUid().trim());

		// Retrieve by email.
		RetrievalResult retrievalResult = emailRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
		pubMedArticles.putAll(retrievalResult.getPubMedArticles());
		slf4jLogger.info("pubMedArticles in retrieveData section with date range****"+pubMedArticles.size());
		/*if (pubMedArticles.size() > 0) {
			Map<Long, AuthorName> aliasSet = AuthorNameUtils.calculatePotentialAlias(identity, pubMedArticles.values());

			slf4jLogger.info("Found " + aliasSet.size() + " new alias for uid=[" + uid + "]");

			// Update alias.
			List<PubMedAlias> pubMedAliases = new ArrayList<PubMedAlias>();
			for (Map.Entry<Long, AuthorName> entry : aliasSet.entrySet()) {
				PubMedAlias pubMedAlias = new PubMedAlias();
				pubMedAlias.setAuthorName(entry.getValue());
				pubMedAlias.setPmid(entry.getKey());
				slf4jLogger.info("new alias for uid=[" + identity.getUid() + "], alias=[" + entry.getValue() + "] from pmid=[" + entry.getKey() + "]");
				pubMedAliases.add(pubMedAlias);
			}

			identity.setPubMedAlias(pubMedAliases);
			// TODO convert to localdate
			Date now = new Date();
			identity.setDateInitialRun(now);
			identity.setDateLastRun(now);
			identityService.save(identity);

			uniquePmids.addAll(pubMedArticles.keySet());
		}*/

		// TODO parallelize by putting save in a separate thread.
		savePubMedArticles(pubMedArticles.values(), uid, emailRetrievalStrategy.getRetrievalStrategyName(), retrievalResult.getPubMedQueryResults(), queryType, refreshFlag);
		uniquePmids.addAll(pubMedArticles.keySet());
		trackNewPmids(pubMedArticles, emailRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

		RetrievalResult r1;
		if(useStrictQueryOnly) {
			r1 = firstNameInitialRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, false);
		} else {
			r1 = firstNameInitialRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
		}
		//if (r1.getPubMedArticles().size() > 0) {
		if(r1.getPubMedQueryResults() != null
				&&
				r1.getPubMedQueryResults().size() > 0
				&&
				r1.getPubMedQueryResults().get(0).getNumResult() < searchStrategyLenientThreshold) {
			if(queryType == null) {
				queryType = QueryType.LENIENT_LOOKUP;
			}
			pubMedArticles.putAll(r1.getPubMedArticles());
			savePubMedArticles(r1.getPubMedArticles().values(), uid, firstNameInitialRetrievalStrategy.getRetrievalStrategyName(), r1.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r1.getPubMedArticles().keySet());
			trackNewPmids(r1.getPubMedArticles(), firstNameInitialRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);
		}

		if(r1.getPubMedQueryResults() != null
				&&
				r1.getPubMedQueryResults().size() > 0
				&&
				r1.getPubMedQueryResults().get(0).getNumResult() > searchStrategyLenientThreshold) {
			queryType = QueryType.STRICT_EXCEEDS_THRESHOLD_LOOKUP;

			// Store the true eSearch count for scoring.
			int trueCount = r1.getPubMedQueryResults().get(0).getNumResult();
			eSearchCountService.save(new ESearchCount(uid, trueCount));
			slf4jLogger.info("Stored eSearchCount={} for uid={}", trueCount, uid);
		}

		if(r1.getPubMedQueryResults() != null
				&&
				r1.getPubMedQueryResults().size() > 0
				&&
				r1.getPubMedQueryResults().get(0).getNumResult() > searchStrategyLenientThreshold
				||
				useStrictQueryOnly) {

			if(identity.getInstitutions() != null && !identity.getInstitutions().isEmpty()) {
				RetrievalResult r2 = affiliationInDbRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
				pubMedArticles.putAll(r2.getPubMedArticles());
				savePubMedArticles(r2.getPubMedArticles().values(), uid, affiliationInDbRetrievalStrategy.getRetrievalStrategyName(), r2.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r2.getPubMedArticles().keySet());
				trackNewPmids(r2.getPubMedArticles(), affiliationInDbRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);
			} else {
				slf4jLogger.info("Skipping " + affiliationInDbRetrievalStrategy.getRetrievalStrategyName() + " since no affiliation for " + identity.getUid());
			}

			RetrievalResult r3 = affiliationRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
			pubMedArticles.putAll(r3.getPubMedArticles());
			savePubMedArticles(r3.getPubMedArticles().values(), uid, affiliationRetrievalStrategy.getRetrievalStrategyName(), r3.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r3.getPubMedArticles().keySet());
			trackNewPmids(r3.getPubMedArticles(), affiliationRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			if(identity.getOrganizationalUnits() != null && !identity.getOrganizationalUnits().isEmpty()) {
				RetrievalResult r4 = departmentRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
				pubMedArticles.putAll(r4.getPubMedArticles());
				savePubMedArticles(r4.getPubMedArticles().values(), uid, departmentRetrievalStrategy.getRetrievalStrategyName(), r4.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r4.getPubMedArticles().keySet());
				trackNewPmids(r4.getPubMedArticles(), departmentRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			} else {
				slf4jLogger.info("Skipping " + departmentRetrievalStrategy.getRetrievalStrategyName() + " since no departments for " + identity.getUid());
			}

			if(identity.getGrants() != null && !identity.getGrants().isEmpty()) {
				RetrievalResult r5 = grantRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
				pubMedArticles.putAll(r5.getPubMedArticles());
				savePubMedArticles(r5.getPubMedArticles().values(), uid, grantRetrievalStrategy.getRetrievalStrategyName(), r5.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r5.getPubMedArticles().keySet());
				trackNewPmids(r5.getPubMedArticles(), grantRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);
			} else {
				slf4jLogger.info("Skipping " + grantRetrievalStrategy.getRetrievalStrategyName() + " since no grants for " + identity.getUid());
			}

			RetrievalResult r6 = fullNameRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
			pubMedArticles.putAll(r6.getPubMedArticles());
			savePubMedArticles(r6.getPubMedArticles().values(), uid, fullNameRetrievalStrategy.getRetrievalStrategyName(), r6.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r6.getPubMedArticles().keySet());
			trackNewPmids(r6.getPubMedArticles(), fullNameRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);

			if(identity.getKnownRelationships() != null && !identity.getKnownRelationships().isEmpty()) {
				RetrievalResult r7 = knownRelationshipRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
				pubMedArticles.putAll(r7.getPubMedArticles());
				savePubMedArticles(r7.getPubMedArticles().values(), uid, knownRelationshipRetrievalStrategy.getRetrievalStrategyName(), r7.getPubMedQueryResults(), queryType, refreshFlag);
				uniquePmids.addAll(r7.getPubMedArticles().keySet());
				trackNewPmids(r7.getPubMedArticles(), knownRelationshipRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);
			} else {
				slf4jLogger.info("Skipping " + knownRelationshipRetrievalStrategy.getRetrievalStrategyName() + " since no Known Relationships for " + identity.getUid());
			}
			RetrievalResult r8 = secondIntialRetrievalStrategy.retrievePubMedArticles(identity, identityNames, startDate, endDate, useStrictQueryOnly);
			pubMedArticles.putAll(r8.getPubMedArticles());
			savePubMedArticles(r8.getPubMedArticles().values(), uid, secondIntialRetrievalStrategy.getRetrievalStrategyName(), r8.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(r8.getPubMedArticles().keySet());
			trackNewPmids(r8.getPubMedArticles(), secondIntialRetrievalStrategy.getRetrievalStrategyName(), uid, existingPmids, newPmidStrategy, backfillPmids);
		}

		// Phase 36 FIX-05: GoldStandard runs LAST with dedup against uniquePmids.
		// Date-range variant uses the dateRange-aware buildQuery overload internally —
		// here we keep the no-args buildQueryGoldStandard helper because date-range and
		// PMID-list semantics don't compose: a PMID is a unique identifier, not a temporal
		// query, so we want all knownPmids retrievable regardless of date window.
		List<PubMedQueryType> gsQueries = goldStandardRetrievalStrategy.buildQueryGoldStandard(identity, uniquePmids);
		RetrievalResult goldStandardRetrievalResult =
				goldStandardRetrievalStrategy.retrievePubMedArticlesUsingQueries(identity, gsQueries, useStrictQueryOnly);
		pubMedArticles.putAll(goldStandardRetrievalResult.getPubMedArticles());
		savePubMedArticles(goldStandardRetrievalResult.getPubMedArticles().values(), uid,
				goldStandardRetrievalStrategy.getRetrievalStrategyName(),
				goldStandardRetrievalResult.getPubMedQueryResults(), queryType, refreshFlag);
		uniquePmids.addAll(goldStandardRetrievalResult.getPubMedArticles().keySet());
		trackNewPmids(goldStandardRetrievalResult.getPubMedArticles(),
				goldStandardRetrievalStrategy.getRetrievalStrategyName(),
				uid, existingPmids, newPmidStrategy, backfillPmids);

		// Retrieve by ORCID — inference now sees GS articles in pubMedArticles (FIX-05).
		String orcidForRetrieval = null;
		if (identity.getOrcid() != null && !identity.getOrcid().isEmpty()
				&& !"NOT SET".equalsIgnoreCase(identity.getOrcid().trim())) {
			orcidForRetrieval = identity.getOrcid().trim();
			slf4jLogger.info("Using asserted ORCID [{}] for uid=[{}]", orcidForRetrieval, uid);
		}
		if (orcidForRetrieval == null) {
			orcidForRetrieval = inferOrcidFromAcceptedArticles(pubMedArticles, goldStandard, identity);
			if (orcidForRetrieval != null) {
				slf4jLogger.info("Inferred ORCID [{}] from accepted articles for uid=[{}]",
						orcidForRetrieval, uid);
				identity.setOrcid(orcidForRetrieval);
				slf4jLogger.info("Using inferred ORCID [{}] (in-memory only) for retrieval, uid=[{}]",
						orcidForRetrieval, uid);
			}
		}
		if (orcidForRetrieval != null) {
			RetrievalResult orcidResult = orcidRetrievalStrategy.retrievePubMedArticles(
					identity, identityNames, startDate, endDate, useStrictQueryOnly);
			pubMedArticles.putAll(orcidResult.getPubMedArticles());
			savePubMedArticles(orcidResult.getPubMedArticles().values(), uid,
					orcidRetrievalStrategy.getRetrievalStrategyName(),
					orcidResult.getPubMedQueryResults(), queryType, refreshFlag);
			uniquePmids.addAll(orcidResult.getPubMedArticles().keySet());
			trackNewPmids(orcidResult.getPubMedArticles(),
					orcidRetrievalStrategy.getRetrievalStrategyName(),
					uid, existingPmids, newPmidStrategy, backfillPmids);
		}

		// Phase 1: Write provenance records for newly discovered PMIDs (no ESearchCount always-save for date-range runs)
		if (!newPmidStrategy.isEmpty()) {
			Date now = new Date();
			List<PmidProvenance> provenanceRecords = new ArrayList<>();
			for (Map.Entry<Long, String> entry : newPmidStrategy.entrySet()) {
				provenanceRecords.add(new PmidProvenance(uid, entry.getKey(), Instant.now(), entry.getValue()));
			}
			pmidProvenanceService.saveAllIfNotExists(provenanceRecords);
			slf4jLogger.info("Wrote {} provenance records (date-range) for uid={}", provenanceRecords.size(), uid);
		}

		slf4jLogger.info("uniquePmids in retrieveData section with date range****"+uniquePmids.size());
		//List<ScopusArticle> scopusArticles = emailRetrievalStrategy.retrieveScopus(uniquePmids);
		//scopusService.save(scopusArticles);
		if (useScopusArticles) {
			List<ScopusArticle> scopusArticles = emailRetrievalStrategy.retrieveScopus(uniquePmids);

			scopusService.save(scopusArticles);

			// Look up the remaining Scopus articles by DOI.
			List<Long> notFoundPmids = new ArrayList<>();
			Set<Long> foundPmids = new HashSet<>();
			for (ScopusArticle scopusArticle : scopusArticles) {
				foundPmids.add(scopusArticle.getPubmedId());
			}
			// Find the pmids that were not found by using pmid query to Scopus.
			for (long pmid : uniquePmids) {
				if (!foundPmids.contains(pmid)) {
					notFoundPmids.add(pmid);
				}
			}

			slf4jLogger.info("Scopus PMID lookup for uid=[{}]: queried={}, matched={}, notFound={}",
					uid, uniquePmids.size(), foundPmids.size(), notFoundPmids.size());

			List<String> dois = new ArrayList<>();
			Map<String, Long> doiToPmid = new HashMap<>();
			int noDoisCount = 0;
			for (long pmid : notFoundPmids) {
				PubMedArticle pubMedArticle = pubMedArticles.get(pmid);

				if (pubMedArticle != null &&
						pubMedArticle.getMedlinecitation() != null &&
						pubMedArticle.getMedlinecitation().getArticle() != null &&
						pubMedArticle.getMedlinecitation().getArticle().getElocationid() != null &&
						pubMedArticle.getMedlinecitation().getArticle().getElocationid().getElocationid() != null) {
					String doi = pubMedArticle.getMedlinecitation().getArticle().getElocationid().getElocationid().toLowerCase();
					dois.add(doi);
					doiToPmid.put(doi, pmid);
				} else {
					noDoisCount++;
				}
			}

			slf4jLogger.info("Scopus DOI fallback for uid=[{}]: notFoundPmids={}, withDoi={}, withoutDoi={}",
					uid, notFoundPmids.size(), dois.size(), noDoisCount);

			List<ScopusArticle> scopusArticlesByDoi = emailRetrievalStrategy.retrieveScopusDoi(dois);
			List<Long> pmidsByDoi = new ArrayList<>();
			int doiMatchSuccess = 0;
			int doiMatchFailed = 0;
			for (ScopusArticle scopusArticle : scopusArticlesByDoi) {
				// manually insert PMID information.
				if (scopusArticle.getDoi() != null && !scopusArticle.getDoi().isEmpty()) {
					if(doiToPmid.get(scopusArticle.getDoi().toLowerCase()) != null) {
						scopusArticle.setPubmedId(doiToPmid.get(scopusArticle.getDoi().toLowerCase()));
						doiMatchSuccess++;
					} else {
						slf4jLogger.warn("Scopus DOI fallback: DOI mismatch for uid=[{}] — Scopus returned doi=[{}] which has no reverse PMID mapping",
								uid, scopusArticle.getDoi());
						doiMatchFailed++;
					}
				} else {
					doiMatchFailed++;
				}
				pmidsByDoi.add(scopusArticle.getPubmedId());
			}

			slf4jLogger.info("Scopus DOI fallback results for uid=[{}]: doisQueried={}, scopusReturned={}, pmidInjected={}, pmidFailed={}, stillUnmatched={}",
					uid, dois.size(), scopusArticlesByDoi.size(), doiMatchSuccess, doiMatchFailed,
					notFoundPmids.size() - doiMatchSuccess);

			scopusService.save(scopusArticlesByDoi);
		}
		slf4jLogger.info("Finished retrieval for uid=[{}], uniquePmids={}", identity.getUid(), uniquePmids.size());
		} finally {
			// Watermark guard (#689): ALWAYS runs — even if a strategy threw after savePubMedArticles
			// already advanced retrievalDate — so a failed run never leaves the watermark skipped past
			// a silently-dropped window. This guard covers the ONLY_NEWLY_ADDED path only:
			// ALL_PUBLICATIONS runs — manual, escalated (#696), or auto-upgraded — never route here
			// (AsyncRetrievalEngine dispatches them to retrieveData), so this rollback does NOT protect
			// them. Their failure handling lives in retrieveData instead: a sweep that hit the swallow
			// path skips the lastFullSweep stamp, so the person stays due and the sweep is retried.
			// The rollback is self-guarded so a DynamoDB hiccup here cannot mask the original exception.
			if (RetrievalErrorTracker.hadError() && preRunRetrievalDate != null) {
				try {
					reciter.database.dynamodb.model.ESearchResult esr = eSearchResultService.findByUid(uid);
					if (esr != null) {
						esr.setRetrievalDate(preRunRetrievalDate);
						eSearchResultService.save(esr);
						slf4jLogger.warn("Retrieval for uid=[{}] hit PubMed failures; rolled retrievalDate back to [{}] "
								+ "so the next ONLY_NEWLY_ADDED run re-covers this window.", uid, preRunRetrievalDate);
					}
				} catch (Exception rollbackEx) {
					slf4jLogger.error("Failed to roll retrievalDate back for uid=[{}]", uid, rollbackEx);
				}
			}
		}

	}
	
	

	@Override
	public void retrieveByPmids(String uid, List<Long> pmids) throws IOException {
		//Not being used
		/*if (!pmids.isEmpty()) {
			RetrievalResult result = goldStandardRetrievalStrategy.retrievePubMedArticles(pmids);
			if (result.getPubMedArticles().size() > 0) {
				savePubMedArticles(result.getPubMedArticles().values(), uid, 
						goldStandardRetrievalStrategy.getRetrievalStrategyName(), result.getPubMedQueryResults(), null);
			}
			List<ScopusArticle> scopusArticles = goldStandardRetrievalStrategy.retrieveScopus(pmids);
			scopusService.save(scopusArticles);
		}*/
	}
	
	/**
	 * Infer the target author's ORCID by scanning accepted (known) PubMed articles.
	 * Uses the full TargetAuthorSelection pipeline (19-step name matching cascade)
	 * to identify the target author on each accepted article, then extracts
	 * the ORCID from whichever author is identified. Returns the most common
	 * matching ORCID, or null if none found.
	 */
	private String inferOrcidFromAcceptedArticles(Map<Long, PubMedArticle> pubMedArticles,
			GoldStandard goldStandard, Identity identity) {
		if (goldStandard == null || goldStandard.getKnownPmids() == null
				|| goldStandard.getKnownPmids().isEmpty() || pubMedArticles == null) {
			return null;
		}

		// Sanitize identity names (same as the scoring pipeline does)
		AuthorNameSanitizationUtils sanitizationUtils = new AuthorNameSanitizationUtils(strategyParameters);
		identity.setSanitizedNames(sanitizationUtils.sanitizeIdentityAuthorNames(identity));

		// Translate accepted PubMed articles into ReCiterArticles and run target author selection
		List<ReCiterArticle> acceptedReCiterArticles = new ArrayList<>();
		for (Long knownPmid : goldStandard.getKnownPmids()) {
			PubMedArticle pubMedArticle = pubMedArticles.get(knownPmid);
			if (pubMedArticle == null) {
				continue;
			}
			try {
				ReCiterArticle reCiterArticle = ArticleTranslator.translate(
						pubMedArticle, null,
						strategyParameters.getNameIgnoredCoAuthors(),
						strategyParameters);
				// Sanitize article author names
				reCiterArticle.getArticleCoAuthors().setSanitizedAuthorMap(
						sanitizationUtils.sanitizeArticleAuthorNames(reCiterArticle));
				acceptedReCiterArticles.add(reCiterArticle);
			} catch (Exception e) {
				slf4jLogger.warn("Could not translate PMID {} for ORCID inference: {}",
						knownPmid, e.getMessage());
			}
		}

		if (acceptedReCiterArticles.isEmpty()) {
			return null;
		}

		// Run the full target author identification (19-step cascade)
		TargetAuthorSelection targetAuthorSelection = new TargetAuthorSelection();
		targetAuthorSelection.identifyTargetAuthor(acceptedReCiterArticles, identity);

		// Collect ORCIDs from identified target authors
		Map<String, Integer> orcidCounts = new HashMap<>();
		for (ReCiterArticle article : acceptedReCiterArticles) {
			if (article.getArticleCoAuthors() == null
					|| article.getArticleCoAuthors().getAuthors() == null) {
				continue;
			}
			for (ReCiterAuthor author : article.getArticleCoAuthors().getAuthors()) {
				if (author.isTargetAuthor()
						&& author.getOrcid() != null
						&& !author.getOrcid().isEmpty()) {
					orcidCounts.merge(author.getOrcid(), 1, Integer::sum);
				}
			}
		}

		if (orcidCounts.isEmpty()) {
			return null;
		}

		// Return the most common ORCID
		return orcidCounts.entrySet().stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse(null);
	}

	/**
	 * Track PMIDs returned by a retrieval strategy.
	 *
	 * <p>Two effects per PMID:
	 * <ol>
	 *   <li><b>Legacy {@code PmidProvenance} attribution</b>: if the PMID is genuinely new
	 *       (not in {@code existingPmids} nor already attributed in {@code newPmidStrategy}),
	 *       record this strategy as the first finder. The contents of {@code newPmidStrategy}
	 *       are batch-written to {@code PmidProvenance} after all strategies finish.
	 *       Backfill-tagged PMIDs are healed via {@link PmidProvenanceService#updateStrategyIfBackfill}.</li>
	 *   <li><b>Phase 33 {@code ArticleProvenance} write</b>: for EVERY PMID returned by the
	 *       strategy (not just new ones), upsert the {@code ArticleProvenance} row with
	 *       {@code rs}/{@code frd}/{@code ads}. Uses {@code if_not_exists} so original
	 *       attribution is preserved on re-runs; {@code ads} grows as new strategies re-find
	 *       this PMID. The {@code src} field is NOT touched here (curator/CTSC paths own it).
	 *       This closes the established-user gap where PmidProvenance was never written
	 *       because all PMIDs were already known.</li>
	 * </ol>
	 *
	 * <p>Phase 33-01 D-01..D-04. Provenance write failures never propagate (logged in service).
	 */
	private void trackNewPmids(Map<Long, ?> articles, String strategyName,
			String uid, Set<Long> existingPmids, Map<Long, String> newPmidStrategy,
			Set<Long> backfillPmids) {
		long nowEpochSeconds = System.currentTimeMillis() / 1000L;
		for (Long pmid : articles.keySet()) {
			// Legacy PmidProvenance attribution (new-only)
			if (!existingPmids.contains(pmid) && !newPmidStrategy.containsKey(pmid)) {
				newPmidStrategy.put(pmid, strategyName);
			}
			if (backfillPmids.contains(pmid)) {
				pmidProvenanceService.updateStrategyIfBackfill(uid, pmid, strategyName);
				backfillPmids.remove(pmid);
			}
			// Phase 33-01: ArticleProvenance upsert for EVERY retrieved PMID
			articleProvenanceService.upsertRetrievalProvenance(uid, pmid, strategyName, nowEpochSeconds);
		}
	}

	/**
	 * This function get all authorNames and derive additional names as well.
	 * @see <a href ="https://github.com/wcmc-its/ReCiter/issues/259">All Identity Name Sec 3.</a>
	 * @param identity
	 * @return
	 */
	void identityAuthorNames(Identity identity, Map<IdentityNameType, Set<AuthorName>> identityNames) {
		Set<AuthorName> identityAuthorNames  = new HashSet<AuthorName>();
		Set<AuthorName> identityDerivedNames = new HashSet<AuthorName>();
		AuthorName identityPrimaryName = identity.getPrimaryName();
		if(identityPrimaryName!=null && identityPrimaryName.getFirstName()!=null && identityPrimaryName.getFirstName()!="")
			identityPrimaryName.setFirstName(ReCiterStringUtil.deAccent(identityPrimaryName.getFirstName().replaceAll("[\"()]", "")));
		if(identityPrimaryName!=null  && identityPrimaryName.getLastName()!=null && identityPrimaryName.getLastName()!="")
			identityPrimaryName.setLastName(ReCiterStringUtil.deAccent(identityPrimaryName.getLastName().replaceAll("(,Jr|, Jr|, MD PhD|,MD PhD|, MD-PhD|,MD-PhD|, PhD|,PhD|, MD|,MD|, III|,III|, II|,II|, Sr|,Sr|Jr|MD PhD|MD-PhD|PhD|MD|III|II|Sr)$", "")));
		if(identityPrimaryName!=null && identityPrimaryName.getMiddleName()!=null && identityPrimaryName.getMiddleName() != "") {
			identityPrimaryName.setMiddleName(ReCiterStringUtil.deAccent(identityPrimaryName.getMiddleName()));
		}
		
		//For any name in primaryName or alternateNames, does targetAuthor have a surname, which satisfies these conditions: 
		//contains a space or dash; if you break up the name at the first space or dash, there would be two strings of four characters or greater
		if(identityPrimaryName!=null &&(identityPrimaryName.getLastName().contains(" ") || identityPrimaryName.getLastName().contains("-")
				||
				(identityPrimaryName.getFirstName().contains(" ") || identityPrimaryName.getFirstName().contains("."))//Cases for W. Clay[firstName] Bracken[lastName]- W.[firstName] Clay[middleName] Bracken[lastName] - W Clay[firstName] Bracken[lastName]
				||
				(identityPrimaryName.getFirstName().length() ==1 && identityPrimaryName.getMiddleName() != null)
				)) {
			identityDerivedNames.addAll(deriveAdditionalName(identityPrimaryName));
		}
		
		identityAuthorNames.add(identityPrimaryName);
		
		if(identity.getAlternateNames() != null) {
			for(AuthorName authorName: identity.getAlternateNames()) {
				// An alternate name deserialized from the Identity table can carry a null
				// firstName or lastName, which every dereference below would NPE on — skip it.
				if(authorName == null || authorName.getFirstName() == null || authorName.getLastName() == null) {
					slf4jLogger.warn("uid: {} has a malformed alternate name (null firstName or lastName); skipping it", identity.getUid());
					continue;
				}
				authorName.setFirstName(ReCiterStringUtil.deAccent(authorName.getFirstName().replaceAll("[\"()]", "")));
				authorName.setLastName(ReCiterStringUtil.deAccent(authorName.getLastName().replaceAll("(,Jr|, Jr|, MD PhD|,MD PhD|, MD-PhD|,MD-PhD|, PhD|,PhD|, MD|,MD|, III|,III|, II|,II|, Sr|,Sr|Jr|MD PhD|MD-PhD|PhD|MD|III|II|Sr)$", "")));
				if(authorName.getMiddleName() != null) {
					authorName.setMiddleName(ReCiterStringUtil.deAccent(authorName.getMiddleName()));
				}
				if(authorName.getLastName().contains(" ") || authorName.getLastName().contains("-")
						||
						(authorName.getFirstName().contains(" ") || authorName.getFirstName().contains("."))
						||
						(authorName.getFirstName().length() ==1 && authorName.getMiddleName() != null)) {
					identityDerivedNames.addAll(deriveAdditionalName(authorName));
				}
				
				identityAuthorNames.add(authorName);
			}
		}
		identityNames.put(IdentityNameType.ORIGINAL, identityAuthorNames);
		identityNames.put(IdentityNameType.DERIVED, identityDerivedNames);
	}
	
	/**
	 * This function derive additional names, if possible.
	 * @see <a href ="https://github.com/wcmc-its/ReCiter/issues/259">Additional Name Sec 4.</a>
	 * @param identityName
	 * @return
	 */
	Set<AuthorName> deriveAdditionalName(AuthorName identityName) {

		Set<AuthorName> derivedAuthorNames = new HashSet<AuthorName>();
		// AuthorName's constructor takes substring(0, 1) of any non-null middleName, so a
		// blank one (an Identity primaryName can carry middleName "") must behave exactly
		// like null — both as a constructor argument and as a first-name substitute below.
		String middleName = identityName.getMiddleName();
		if(middleName != null && middleName.isBlank()) {
			middleName = null;
		}
		// A malformed Identity can carry a null first or last name (see #715/#717), and an
		// unchecked throw here dies inside a retrieval worker — the swallowed-crash shape
		// this PR exists to stop. Read each once, guard each once.
		String lastName = identityName.getLastName();
		String firstName = identityName.getFirstName();
		if(lastName != null && !lastName.isBlank()
				&& (lastName.contains(" ") || lastName.contains("."))) {
			// The pattern splits on whitespace and hyphens, never on a period, so a
			// period-without-space surname ("St.John") enters this branch but splits into a
			// single element. Require both halves before indexing either one.
			String[] possibleLastName = lastName.split("\\s+|-", 2);
			if(possibleLastName.length > 1
					&&
					possibleLastName[0].length() >=4
					&&
					possibleLastName[1].length() >=4) {

				AuthorName authorName1 = new AuthorName(firstName, middleName, possibleLastName[0].trim());
				AuthorName authorName2 = new AuthorName(firstName, middleName, possibleLastName[1].trim());
				derivedAuthorNames.add(authorName1);
				derivedAuthorNames.add(authorName2);
			}
		}
		if(firstName != null && (firstName.contains(" ") || firstName.contains("."))) {
			// Measure the trimmed value: "W. " is the same initial as "W.", which an
			// untrimmed length check silently skips. A genuine one-letter first name has
			// neither a space nor a period, so it never reaches this branch at all.
			String trimmedFirstName = firstName.trim();
			if(trimmedFirstName.length() ==2 && trimmedFirstName.endsWith(".") && middleName != null) {
				AuthorName authorName1 = new AuthorName(middleName, null, lastName);//W.[firstName] Clay[middleName] Bracken[lastName]
				derivedAuthorNames.add(authorName1);
			}
			if(firstName.length() >=3 && Character.isWhitespace(firstName.charAt(1))) {
				//String[] possibleFirstName = identityName.getFirstName().split("\\s+", 2);
				AuthorName authorName1 = new AuthorName(Character.toString(firstName.charAt(2)), middleName, lastName);//W Clay[firstName] Bracken[lastName]
				derivedAuthorNames.add(authorName1);
			}	
			if(firstName.length() >=4 && Character.isWhitespace(firstName.charAt(1)) && firstName.charAt(2) == '.') {
				//String[] possibleFirstName = identityName.getFirstName().split(".\\s+", 2);
				AuthorName authorName1 = new AuthorName(Character.toString(firstName.charAt(3)), middleName, lastName);//W. Clay[firstName] Bracken[lastName]
				derivedAuthorNames.add(authorName1);
			}
		}
		if(firstName != null && firstName.length() ==1 && middleName != null) {//Case for W[firstName] Clay[middleName] Bracken[lastName]
			AuthorName authorName1 = new AuthorName(middleName, null, lastName);
			derivedAuthorNames.add(authorName1);
		}
		
		return derivedAuthorNames;
	}
}
