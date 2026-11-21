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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reciter.api.parameters.RetrievalRefreshFlag;
import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;
import reciter.database.dynamodb.model.QueryType;
import reciter.engine.StrategyParameters;
import reciter.model.pubmed.PubMedArticle;
import reciter.service.ESearchResultService;
import reciter.service.IdentityService;
import reciter.service.PubMedService;
import reciter.service.ScopusService;
import reciter.xml.retriever.pubmed.AffiliationInDbRetrievalStrategy;
import reciter.xml.retriever.pubmed.AffiliationRetrievalStrategy;
import reciter.xml.retriever.pubmed.DepartmentRetrievalStrategy;
import reciter.xml.retriever.pubmed.EmailRetrievalStrategy;
import reciter.xml.retriever.pubmed.FirstNameInitialRetrievalStrategy;
import reciter.xml.retriever.pubmed.FullNameRetrievalStrategy;
import reciter.xml.retriever.pubmed.GoldStandardRetrievalStrategy;
import reciter.xml.retriever.pubmed.GrantRetrievalStrategy;
import reciter.xml.retriever.pubmed.KnownRelationshipRetrievalStrategy;
import reciter.xml.retriever.pubmed.OrcidRetrievalStrategy;
import reciter.xml.retriever.pubmed.PubMedQueryResult;
import reciter.xml.retriever.pubmed.SecondInitialRetrievalStrategy;

@Component("abstractReCiterRetrievalEngine")
public abstract class AbstractReCiterRetrievalEngine implements ReCiterRetrievalEngine {
	
	private static final Logger log = LoggerFactory.getLogger(AbstractReCiterRetrievalEngine.class);

	@Autowired
	protected PubMedService pubMedService;

	@Autowired
	protected ESearchResultService eSearchResultService;

	@Autowired
	protected ScopusService scopusService;

	@Autowired
	protected IdentityService identityService;

	@Autowired
	protected StrategyParameters strategyParameters;

	@Autowired
	protected AffiliationInDbRetrievalStrategy affiliationInDbRetrievalStrategy;
	
	@Autowired
	protected AffiliationRetrievalStrategy affiliationRetrievalStrategy;
	
	@Autowired
	protected DepartmentRetrievalStrategy departmentRetrievalStrategy;
	
	@Autowired
	protected EmailRetrievalStrategy emailRetrievalStrategy;
	
	@Autowired
	protected FirstNameInitialRetrievalStrategy firstNameInitialRetrievalStrategy;
	
	@Autowired
	protected FullNameRetrievalStrategy fullNameRetrievalStrategy;
	
	@Autowired
	protected SecondInitialRetrievalStrategy secondIntialRetrievalStrategy;
	
	@Autowired
	protected KnownRelationshipRetrievalStrategy knownRelationshipRetrievalStrategy;
	
	@Autowired
	protected GoldStandardRetrievalStrategy goldStandardRetrievalStrategy;

	@Autowired
	protected OrcidRetrievalStrategy orcidRetrievalStrategy;

	@Autowired
	protected GrantRetrievalStrategy grantRetrievalStrategy;
	
	/**
	 * Save the PubMed articles and the ESearch results.
	 * @param pubMedArticles
	 * @param uid
	 */
	protected void savePubMedArticles(Collection<PubMedArticle> pubMedArticles, String uid, String retrievalStrategyName, List<PubMedQueryResult> pubMedQueryResults, QueryType queryType, RetrievalRefreshFlag refreshFlag) {
		// Read the existing search result first: it receives the upserted strategy entry
		// below, and it also tells us which pmids this uid already has persisted.
		ESearchResult eSearchResultDb = eSearchResultService.findByUid(uid);

		// Save the articles. On incremental runs, skip articles whose pmid is already on
		// the uid's ESearchResult (#695): the floored lookback window re-retrieves the same
		// span nightly, and without this every run would re-write that whole span to
		// DynamoDB/S3. Full sweeps still re-persist everything, so revised PubMed records
		// are refreshed at sweep cadence.
		List<PubMedArticle> pubMedArticleList = articlesToPersist(pubMedArticles, eSearchResultDb, refreshFlag);
		log.info("pubMedArticleList size {} (retrieved {})", pubMedArticleList.size(), pubMedArticles.size());
		pubMedService.save(pubMedArticleList);

		// Save the search result. The strategy entry always records every retrieved pmid,
		// including ones whose article write was skipped above.
		List<Long> pmids = new ArrayList<>();
		for (PubMedArticle pubMedArticle : pubMedArticles) {
			pmids.add(pubMedArticle.getMedlinecitation().getMedlinecitationpmid().getPmid());
		}

		ESearchPmid eSearchPmid = null;
		if(!pmids.isEmpty()){
			reciter.database.dynamodb.model.ESearchPmid.RetrievalRefreshFlag eSearchPmidRefreshFlag;
			if(refreshFlag == RetrievalRefreshFlag.ALL_PUBLICATIONS) {
				eSearchPmidRefreshFlag = reciter.database.dynamodb.model.ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS;
			} else if(refreshFlag == RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS) {
				eSearchPmidRefreshFlag = reciter.database.dynamodb.model.ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS;
			} else {
				eSearchPmidRefreshFlag = reciter.database.dynamodb.model.ESearchPmid.RetrievalRefreshFlag.FALSE;
			}
			eSearchPmid = new ESearchPmid(pmids, retrievalStrategyName, Instant.now(), eSearchPmidRefreshFlag);
			if(eSearchPmid != null) {
				log.info("eSearchPmid {} ",eSearchPmid);
			}
		}
		if (eSearchResultDb == null) {
			List<ESearchPmid> eSearchPmids = new ArrayList<>();
			if(eSearchPmid != null) {
				eSearchPmids.add(eSearchPmid);
			}
			if(!eSearchPmids.isEmpty()) {
				eSearchResultService.save(new ESearchResult(uid, Instant.now(), eSearchPmids, queryType));
			}
		} else {
			List<ESearchPmid> eSearchPmids = eSearchResultDb.getESearchPmids();
			if(eSearchPmid != null) {
				// FIX (#640-B): upsert by retrievalStrategyName. Previously every re-run
				// appended a new ESearchPmid for the same strategy without removing the prior
				// entry, growing the ESearchResult item unbounded toward the 400KB DynamoDB cap.
				String newStrategyName = eSearchPmid.getRetrievalStrategyName();
				ESearchPmid existingEntry = eSearchPmids.stream()
						.filter(existing -> existing != null
								&& existing.getRetrievalStrategyName() != null
								&& existing.getRetrievalStrategyName().equalsIgnoreCase(newStrategyName))
						.findFirst().orElse(null);
				eSearchPmids.removeIf(existing -> existing != null
						&& existing.getRetrievalStrategyName() != null
						&& existing.getRetrievalStrategyName().equalsIgnoreCase(newStrategyName));
				// #737: a run that hit PubMed failures for this uid must not let a partial
				// batch shrink the stored strategy entry — union instead of replace.
				boolean runHadFailures = reciter.xml.retriever.pubmed.RetrievalErrorTracker.hadError();
				ESearchPmid upserted = upsertedStrategyEntry(existingEntry, eSearchPmid, runHadFailures);
				if (runHadFailures && existingEntry != null
						&& existingEntry.getPmids() != null && eSearchPmid.getPmids() != null) {
					int storedCount = upserted.getPmids() == null ? 0 : upserted.getPmids().size();
					int incomingCount = eSearchPmid.getPmids().size();
					if (storedCount > incomingCount) {
						log.warn("uid=[{}] strategy [{}]: run hit PubMed failures; kept {} prior pmids the run did not return (incoming {}, stored {})",
								uid, newStrategyName, storedCount - incomingCount, incomingCount, storedCount);
					}
				}
				eSearchPmids.add(upserted);
			}
			if(!eSearchPmids.isEmpty()) {
				eSearchResultService.save(new ESearchResult(uid, Instant.now(), eSearchPmids, queryType));
			} else {
				eSearchResultDb.setRetrievalDate(Instant.now());
				eSearchResultDb.setQueryType(queryType);
				eSearchResultService.save(eSearchResultDb);
			}
		}
	}

	/**
	 * The strategy entry to store when a retrieval upserts over an existing one.
	 * Normally the incoming entry replaces the old one wholesale, but two situations
	 * refuse that replacement and merge pmids instead:
	 *
	 * <p>(1) An incremental run must not downgrade an entry's {@code lookupType} from
	 * ALL_PUBLICATIONS (#696/E13): {@code ArticleSizeStrategy} filters entries on that
	 * marker to compute {@code articleCountScore}, so erasing it perturbs scoring, and
	 * the escalation fallback infers last-full-sweep from the same marker's
	 * {@code retrievalDate}. The stored entry keeps the ALL_PUBLICATIONS marker AND the
	 * full sweep's retrievalDate — bumping the date would make the person look freshly
	 * swept and suppress a due escalation — while the pmid lists are merged. This rule
	 * applies regardless of {@code runHadFailures}.
	 *
	 * <p>(2) A run that hit PubMed failures for this uid (#737) must not let a partial
	 * batch shrink the stored entry: on 2026-09-01 3 of 8 GoldStandardRetrievalStrategy
	 * batches for rharrington failed with NCBI connection resets, and the wholesale
	 * replace this method previously always performed shrank the strategy entry from
	 * 794 to 394 pmids — which shrank the Analysis candidate set with it, and cache-only
	 * re-analysis could not heal it since the candidate set is rebuilt from this stored
	 * entry. When {@code runHadFailures} is true the pmids are unioned (existing first,
	 * then incoming's not-yet-seen), the lookupType is escalated to ALL_PUBLICATIONS if
	 * either side already carries it (otherwise incoming's), and retrievalDate is
	 * incoming's — a failed run should still look attempted, unlike case (1)'s refused
	 * sweep. When a strategy returns zero pmids the caller never builds an incoming
	 * entry at all, so a total failure leaves the existing entry untouched (:137, :158);
	 * only a partial batch reaches this union path.
	 *
	 * <p>Outside both cases (clean run, no ALL_PUBLICATIONS downgrade at stake) the
	 * incoming entry replaces the old one wholesale, unchanged from prior behavior.
	 * Pure so both rules are unit-testable — no {@code RetrievalErrorTracker} read here.
	 */
	static ESearchPmid upsertedStrategyEntry(ESearchPmid existing, ESearchPmid incoming, boolean runHadFailures) {
		if (existing == null || incoming == null) {
			return incoming;
		}
		boolean existingIsAll = existing.getLookupType() == ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS;
		boolean incomingIsAll = incoming.getLookupType() == ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS;

		if (existingIsAll && !incomingIsAll) {
			// Rule b: refused ALL_PUBLICATIONS downgrade — merge, keep existing's date.
			List<Long> mergedPmids = mergePmids(existing.getPmids(), incoming.getPmids());
			log.info("Refusing lookupType downgrade for strategy {} : keeping ALL_PUBLICATIONS marker "
					+ "(sweep date {}), merged pmids {} -> {}", incoming.getRetrievalStrategyName(),
					existing.getRetrievalDate(),
					incoming.getPmids() == null ? 0 : incoming.getPmids().size(), mergedPmids.size());
			return new ESearchPmid(mergedPmids, incoming.getRetrievalStrategyName(),
					existing.getRetrievalDate(), ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);
		}

		if (runHadFailures) {
			// Rule c (#737): failed run — union, keep incoming's date.
			List<Long> mergedPmids = mergePmids(existing.getPmids(), incoming.getPmids());
			ESearchPmid.RetrievalRefreshFlag lookupType = (existingIsAll || incomingIsAll)
					? ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS
					: incoming.getLookupType();
			return new ESearchPmid(mergedPmids, incoming.getRetrievalStrategyName(),
					incoming.getRetrievalDate(), lookupType);
		}

		// Rule d: clean run, no ALL_PUBLICATIONS downgrade at stake — replace wholesale.
		return incoming;
	}

	/**
	 * Union of two pmid lists, existing's entries first in their original order, then
	 * incoming's not-yet-seen entries in their original order. Nulls are skipped.
	 */
	private static List<Long> mergePmids(List<Long> existingPmids, List<Long> incomingPmids) {
		List<Long> mergedPmids = new ArrayList<>();
		Set<Long> seen = new HashSet<>();
		for (List<Long> pmidList : Arrays.asList(existingPmids, incomingPmids)) {
			if (pmidList == null) {
				continue;
			}
			for (Long pmid : pmidList) {
				if (pmid != null && seen.add(pmid)) {
					mergedPmids.add(pmid);
				}
			}
		}
		return mergedPmids;
	}

	/**
	 * Stamp {@code lastFullSweep} after an ALL_PUBLICATIONS retrieval — but only when
	 * every strategy completed without entering the error-swallow path (#696). Stamping
	 * after a swallowed failure would lock the loss in until the next scheduled sweep,
	 * 30–365 days away, which is strictly worse than not stamping: an unstamped person
	 * simply stays due and the sweep is retried. The write itself is conditional and
	 * never regresses a newer stamp (see ESearchResultRepository).
	 */
	protected void stampLastFullSweepIfClean(String uid, RetrievalRefreshFlag refreshFlag) {
		if (refreshFlag != RetrievalRefreshFlag.ALL_PUBLICATIONS) {
			return;
		}
		if (reciter.xml.retriever.pubmed.RetrievalErrorTracker.hadError()) {
			log.warn("Full sweep for uid=[{}] hit PubMed failures; NOT stamping lastFullSweep so the "
					+ "sweep stays due and is retried.", uid);
			return;
		}
		eSearchResultService.stampLastFullSweepIfNewer(uid, Instant.now());
	}

	/**
	 * Articles worth persisting from this retrieval. On an ONLY_NEWLY_ADDED run, pmids
	 * already recorded on the uid's ESearchResult (under any strategy) were persisted by
	 * an earlier run and are dropped here; everything else — full sweeps, uids with no
	 * prior result — is persisted unchanged. Pure so the filter is unit-testable.
	 */
	static List<PubMedArticle> articlesToPersist(Collection<PubMedArticle> pubMedArticles, ESearchResult existingResult, RetrievalRefreshFlag refreshFlag) {
		List<PubMedArticle> articles = new ArrayList<>(pubMedArticles);
		if (refreshFlag != RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS
				|| existingResult == null || existingResult.getESearchPmids() == null) {
			return articles;
		}
		Set<Long> knownPmids = new HashSet<>();
		for (ESearchPmid entry : existingResult.getESearchPmids()) {
			if (entry != null && entry.getPmids() != null) {
				knownPmids.addAll(entry.getPmids());
			}
		}
		if (knownPmids.isEmpty()) {
			return articles;
		}
		List<PubMedArticle> toPersist = new ArrayList<>(articles.size());
		for (PubMedArticle article : articles) {
			if (!knownPmids.contains(article.getMedlinecitation().getMedlinecitationpmid().getPmid())) {
				toPersist.add(article);
			}
		}
		return toPersist;
	}
}
