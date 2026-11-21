package reciter.xml.retriever.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import reciter.api.parameters.RetrievalRefreshFlag;
import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;
import reciter.database.dynamodb.model.QueryType;
import reciter.model.identity.Identity;
import reciter.model.pubmed.MedlineCitation;
import reciter.model.pubmed.MedlineCitationPMID;
import reciter.model.pubmed.PubMedArticle;
import reciter.service.ESearchResultService;
import reciter.service.PubMedService;
import reciter.xml.retriever.pubmed.RetrievalErrorTracker;

/**
 * Covers the known-pmid persistence skip (#695): with a floored lookback window the
 * same span is re-retrieved nightly, so incremental runs must not re-write articles
 * the uid's ESearchResult already records. Also covers the #696 lookupType downgrade
 * guard and clean-completion gate, and the #737 no-shrink-on-failure union rule.
 */
public class AbstractReCiterRetrievalEngineTest {

	private static PubMedArticle article(long pmid) {
		MedlineCitationPMID id = new MedlineCitationPMID();
		id.setPmid(pmid);
		MedlineCitation citation = new MedlineCitation();
		citation.setMedlinecitationpmid(id);
		PubMedArticle article = new PubMedArticle();
		article.setMedlinecitation(citation);
		return article;
	}

	private static List<Long> pmidsOf(List<PubMedArticle> articles) {
		return articles.stream()
				.map(a -> a.getMedlinecitation().getMedlinecitationpmid().getPmid())
				.collect(Collectors.toList());
	}

	private static ESearchResult resultWithKnownPmids() {
		ESearchResult existing = new ESearchResult();
		existing.setESearchPmids(Arrays.asList(
				new ESearchPmid(Arrays.asList(1L, 2L), "EmailRetrievalStrategy", Instant.now(),
						ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS),
				new ESearchPmid(Collections.singletonList(3L), "FullNameRetrievalStrategy", Instant.now(),
						ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS)));
		return existing;
	}

	@Test
	public void incrementalRunSkipsPmidsAlreadyOnTheESearchResult() {
		// 2 and 3 are already recorded (under different strategies); only 4 is new.
		List<PubMedArticle> toPersist = AbstractReCiterRetrievalEngine.articlesToPersist(
				Arrays.asList(article(2L), article(3L), article(4L)),
				resultWithKnownPmids(), RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		assertEquals(Collections.singletonList(4L), pmidsOf(toPersist));
	}

	@Test
	public void fullSweepRePersistsEverything() {
		// A full sweep refreshes stored article payloads, so nothing is skipped.
		List<PubMedArticle> toPersist = AbstractReCiterRetrievalEngine.articlesToPersist(
				Arrays.asList(article(2L), article(3L), article(4L)),
				resultWithKnownPmids(), RetrievalRefreshFlag.ALL_PUBLICATIONS);
		assertEquals(Arrays.asList(2L, 3L, 4L), pmidsOf(toPersist));
	}

	@Test
	public void incrementalRunWithNoPriorResultPersistsEverything() {
		List<PubMedArticle> retrieved = Arrays.asList(article(5L), article(6L));

		assertEquals(Arrays.asList(5L, 6L), pmidsOf(AbstractReCiterRetrievalEngine.articlesToPersist(
				retrieved, null, RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS)));
		assertEquals(Arrays.asList(5L, 6L), pmidsOf(AbstractReCiterRetrievalEngine.articlesToPersist(
				retrieved, new ESearchResult(), RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS)));
	}

	@Test
	public void entriesWithoutPmidListsAreTolerated() {
		ESearchResult existing = new ESearchResult();
		existing.setESearchPmids(Arrays.asList(
				null,
				new ESearchPmid(null, "EmailRetrievalStrategy", Instant.now(),
						ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS)));
		List<PubMedArticle> toPersist = AbstractReCiterRetrievalEngine.articlesToPersist(
				Collections.singletonList(article(7L)),
				existing, RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		assertEquals(Collections.singletonList(7L), pmidsOf(toPersist));
	}

	// ---- lookupType downgrade guard (#696 / E13) and #737 no-shrink-on-failure union ----

	private static final Instant SWEEP_DATE = Instant.parse("2026-05-15T00:00:00Z");
	private static final Instant TONIGHT = Instant.parse("2026-08-03T00:00:00Z");

	@Test
	public void incrementalUpsertMustNotDowngradeAnAllPublicationsEntry() {
		// ArticleSizeStrategy counts pmids on entries whose lookupType is
		// ALL_PUBLICATIONS, and the escalation fallback infers last-full-sweep from
		// the same entries. The stored entry must therefore keep the marker AND the
		// sweep's retrievalDate (a bumped date would fake a fresh sweep and suppress
		// a due escalation), while merging in the incremental run's new pmids. This
		// rule applies on a clean run (runHadFailures=false, asserted here) and is
		// independently re-asserted for both boolean values in
		// downgradeGuardIgnoresRunHadFailuresInEitherDirection (#737 case e).
		ESearchPmid existing = new ESearchPmid(Arrays.asList(1L, 2L, 3L), "EmailRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(3L, 4L), "EmailRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		ESearchPmid stored = AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, false);

		assertEquals(ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS, stored.getLookupType());
		assertEquals(SWEEP_DATE, stored.getRetrievalDate());
		assertEquals(Arrays.asList(1L, 2L, 3L, 4L), stored.getPmids());
	}

	@Test
	public void fullSweepUpsertReplacesTheEntryWholesale() {
		ESearchPmid existing = new ESearchPmid(Arrays.asList(1L, 2L), "EmailRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(2L, 5L), "EmailRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);

		assertSame(incoming, AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, false));
	}

	@Test
	public void incrementalOverIncrementalReplacesTheEntry() {
		ESearchPmid existing = new ESearchPmid(Arrays.asList(1L), "EmailRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(2L), "EmailRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		assertSame(incoming, AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, false));
	}

	@Test
	public void firstEntryForAStrategyIsStoredAsIs() {
		// Also #737 case (c): a null existing entry always returns incoming, even
		// when the run had failures.
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(2L), "EmailRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		assertSame(incoming, AbstractReCiterRetrievalEngine.upsertedStrategyEntry(null, incoming, true));
	}

	@Test
	public void downgradeGuardToleratesNullPmidLists() {
		ESearchPmid existing = new ESearchPmid(null, "EmailRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(4L), "EmailRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		ESearchPmid stored = AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, false);
		assertEquals(ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS, stored.getLookupType());
		assertEquals(Arrays.asList(4L), stored.getPmids());
	}

	@Test
	public void failedRunUnionsPmidsWhenNeitherSideIsAllPublications() {
		// #737 case (a): a partial GoldStandardRetrievalStrategy batch during a run that
		// hit PubMed failures must not shrink the ONLY_NEWLY_ADDED entry — union in the
		// prior pmids the run did not return, keep incoming's lookupType and date.
		ESearchPmid existing = new ESearchPmid(Arrays.asList(1L, 2L, 3L, 4L, 5L), "GoldStandardRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(1L, 2L, 3L), "GoldStandardRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		ESearchPmid stored = AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, true);

		assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), stored.getPmids());
		assertEquals(ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS, stored.getLookupType());
		assertEquals(TONIGHT, stored.getRetrievalDate());
	}

	@Test
	public void cleanRunReplacesTheEntryWholesaleEvenWithFewerIncomingPmids() {
		// #737 case (b): same inputs as the case-(a) test above, but a clean run keeps
		// the pre-#737 wholesale-replace behavior — incoming returned as is.
		ESearchPmid existing = new ESearchPmid(Arrays.asList(1L, 2L, 3L, 4L, 5L), "GoldStandardRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(1L, 2L, 3L), "GoldStandardRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		assertSame(incoming, AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, false));
	}

	@Test
	public void failedRunUnionsBothAllPublicationsEntries() {
		// #737 case (d): both sides already carry the ALL_PUBLICATIONS marker, so this
		// is not the downgrade-guard path (that requires incoming to NOT be ALL) — it is
		// the failed-run union, which keeps the ALL marker and takes incoming's date.
		ESearchPmid existing = new ESearchPmid(Arrays.asList(1L, 2L, 3L, 4L, 5L), "EmailRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(1L, 2L), "EmailRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);

		ESearchPmid stored = AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, true);

		assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), stored.getPmids());
		assertEquals(ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS, stored.getLookupType());
		assertEquals(TONIGHT, stored.getRetrievalDate());
	}

	@Test
	public void downgradeGuardIgnoresRunHadFailuresInEitherDirection() {
		// #737 case (e), regression of #696/E13: the ALL_PUBLICATIONS downgrade guard
		// (rule b) takes precedence over the failed-run union (rule c) regardless of
		// runHadFailures — both true and false keep the ALL marker and the EXISTING
		// entry's retrievalDate, merging in the new pmid either way.
		ESearchPmid existing = new ESearchPmid(Arrays.asList(1L, 2L, 3L, 4L, 5L), "EmailRetrievalStrategy",
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS);
		ESearchPmid incoming = new ESearchPmid(Arrays.asList(6L), "EmailRetrievalStrategy",
				TONIGHT, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		for (boolean runHadFailures : Arrays.asList(true, false)) {
			ESearchPmid stored = AbstractReCiterRetrievalEngine.upsertedStrategyEntry(existing, incoming, runHadFailures);
			assertEquals(ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS, stored.getLookupType());
			assertEquals(SWEEP_DATE, stored.getRetrievalDate());
			assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), stored.getPmids());
		}
	}

	// ---- #737 end-to-end: savePubMedArticles reads RetrievalErrorTracker.hadError() ----

	private static ESearchPmid gsEntryOf(ESearchResult result, String strategyName) {
		return result.getESearchPmids().stream()
				.filter(e -> e != null && strategyName.equalsIgnoreCase(e.getRetrievalStrategyName()))
				.findFirst().orElse(null);
	}

	@Test
	public void savePubMedArticlesKeepsPriorPmidsOnlyWhenTheRunHadFailures() {
		// #737 case (f): the rharrington shape — a GoldStandardRetrievalStrategy entry
		// with 5 prior pmids, a run that only came back with 3 of them. When the run hit
		// PubMed failures the stored entry must keep all 5; on a clean re-run with the
		// same partial batch, it replaces wholesale down to 3 as before.
		ESearchResultService eSearchResultService = mock(ESearchResultService.class);
		PubMedService pubMedService = mock(PubMedService.class);
		String uid = "rharrington";
		String strategyName = "GoldStandardRetrievalStrategy";

		ESearchPmid gsEntry = new ESearchPmid(new ArrayList<>(Arrays.asList(1L, 2L, 3L, 4L, 5L)), strategyName,
				SWEEP_DATE, ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		ESearchResult existingResult = new ESearchResult(uid, SWEEP_DATE,
				new ArrayList<>(Arrays.asList(gsEntry)), QueryType.LENIENT_LOOKUP);
		when(eSearchResultService.findByUid(uid)).thenReturn(existingResult);

		AbstractReCiterRetrievalEngine engine = engineWith(eSearchResultService);
		engine.pubMedService = pubMedService;

		List<PubMedArticle> retrieved = Arrays.asList(article(1L), article(2L), article(3L));

		RetrievalErrorTracker.reset();
		RetrievalErrorTracker.markError();
		engine.savePubMedArticles(retrieved, uid, strategyName, Collections.emptyList(),
				QueryType.LENIENT_LOOKUP, RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		// Capture and snapshot immediately: savePubMedArticles mutates the
		// ESearchResult's pmid list in place, and the mock's findByUid keeps
		// returning that same mutable object, so a captured reference read after
		// the second call below would reflect the second call's state instead of
		// the first's.
		ArgumentCaptor<ESearchResult> firstSave = ArgumentCaptor.forClass(ESearchResult.class);
		verify(eSearchResultService, times(1)).save(firstSave.capture());
		List<Long> afterFailedRunPmids = new ArrayList<>(gsEntryOf(firstSave.getValue(), strategyName).getPmids());
		assertEquals(5, afterFailedRunPmids.size());

		RetrievalErrorTracker.reset();
		engine.savePubMedArticles(retrieved, uid, strategyName, Collections.emptyList(),
				QueryType.LENIENT_LOOKUP, RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);

		ArgumentCaptor<ESearchResult> secondSave = ArgumentCaptor.forClass(ESearchResult.class);
		verify(eSearchResultService, times(2)).save(secondSave.capture());
		ESearchPmid afterCleanRun = gsEntryOf(secondSave.getValue(), strategyName);
		assertEquals(3, afterCleanRun.getPmids().size());
	}

	// ---- Clean-completion gate on the lastFullSweep stamp (#696) ----

	@AfterEach
	public void clearErrorFlag() {
		RetrievalErrorTracker.reset();
	}

	private static AbstractReCiterRetrievalEngine engineWith(ESearchResultService service) {
		AbstractReCiterRetrievalEngine engine = new AbstractReCiterRetrievalEngine() {
			@Override
			public boolean retrieveArticlesByDateRange(List<Identity> identities, java.util.Date startDate,
					java.util.Date endDate, RetrievalRefreshFlag refreshFlag) {
				return true;
			}

			@Override
			public void retrieveByPmids(String uid, List<Long> pmids) {
			}
		};
		engine.eSearchResultService = service;
		return engine;
	}

	@Test
	public void cleanFullSweepStampsLastFullSweep() {
		ESearchResultService service = mock(ESearchResultService.class);
		RetrievalErrorTracker.reset();

		engineWith(service).stampLastFullSweepIfClean("cam4024", RetrievalRefreshFlag.ALL_PUBLICATIONS);

		verify(service).stampLastFullSweepIfNewer(eq("cam4024"), any(Instant.class));
	}

	@Test
	public void sweepThatHitTheSwallowPathIsNotStamped() {
		// Stamping after a swallowed PubMed failure would lock the loss in for a full
		// interval; an unstamped person simply stays due and the sweep is retried.
		ESearchResultService service = mock(ESearchResultService.class);
		RetrievalErrorTracker.reset();
		RetrievalErrorTracker.markError();

		engineWith(service).stampLastFullSweepIfClean("cam4024", RetrievalRefreshFlag.ALL_PUBLICATIONS);

		verify(service, never()).stampLastFullSweepIfNewer(anyString(), any(Instant.class));
	}

	@Test
	public void incrementalRunsNeverStamp() {
		ESearchResultService service = mock(ESearchResultService.class);
		RetrievalErrorTracker.reset();

		engineWith(service).stampLastFullSweepIfClean("cam4024", RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS);
		engineWith(service).stampLastFullSweepIfClean("cam4024", null);

		verify(service, never()).stampLastFullSweepIfNewer(anyString(), any(Instant.class));
	}
}