package reciter.xml.retriever.pubmed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;

public class RetrievalWindowTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 8, 3);

	private static Instant on(int y, int m, int d) {
		return LocalDate.of(y, m, d).atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	private static Instant daysAgo(long days) {
		return TODAY.minusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	@Test
	public void floorAppliesWhenTheUidRanRecently() {
		// Ran yesterday: the legacy watermark window would start 2026-07-30 and miss
		// anything indexed earlier. The floor pulls it back to 90 days.
		assertEquals(LocalDate.of(2026, 5, 5),
				RetrievalWindow.incrementalStart(on(2026, 7, 31), TODAY, 90));
	}

	@Test
	public void watermarkWinsWhenItIsOlderThanTheFloor() {
		// Nine months since the last run: keep reaching back that far, do not shorten
		// the window to the floor.
		assertEquals(LocalDate.of(2025, 10, 31),
				RetrievalWindow.incrementalStart(on(2025, 11, 1), TODAY, 90));
	}

	@Test
	public void dateMissedByTheWatermarkWindowFallsInsideTheFlooredWindow() {
		// A uid that last ran 2026-07-30 gets a watermark window starting 2026-07-29,
		// which excludes an article indexed 2026-06-04 — one missed run and it is gone
		// for good. The floored window must still cover that date.
		LocalDate start = RetrievalWindow.incrementalStart(on(2026, 7, 30), TODAY, 90);
		assertTrue(start.isBefore(LocalDate.of(2026, 6, 4)), "EDAT 2026-06-04 must fall inside the window");
	}

	@Test
	public void windowIsNeverNarrowerThanTheFloor() {
		LocalDate floorStart = TODAY.minusDays(90);
		for (int age = 0; age <= 120; age++) {
			Instant watermark = TODAY.minusDays(age).atStartOfDay(ZoneOffset.UTC).toInstant();
			LocalDate start = RetrievalWindow.incrementalStart(watermark, TODAY, 90);
			assertFalse(start.isAfter(floorStart),
					"watermark " + age + "d old gave window start " + start + ", inside the floor");
		}
	}

	@Test
	public void floorOfZeroKeepsLegacyWatermarkBehaviour() {
		// Legacy window: retrievalDate - 1 day, exactly.
		assertEquals(LocalDate.of(2026, 7, 29),
				RetrievalWindow.incrementalStart(on(2026, 7, 30), TODAY, 0));
		assertEquals(LocalDate.of(2025, 10, 31),
				RetrievalWindow.incrementalStart(on(2025, 11, 1), TODAY, 0));
		// Legacy behaviour with no prior run: yesterday.
		assertEquals(TODAY.minusDays(1), RetrievalWindow.incrementalStart(null, TODAY, 0));
	}

	@Test
	public void nullWatermarkFallsBackToTheFloor() {
		assertEquals(LocalDate.of(2026, 5, 5), RetrievalWindow.incrementalStart(null, TODAY, 90));
	}

	// ---- Full-sweep escalation (#696) ----

	@Test
	public void fullSweepDueWhenLastSweepOlderThanMaxAge() {
		// Jitter 0 keeps the boundary crisp: due exactly at lastFullSweep + maxAge.
		assertTrue(RetrievalWindow.fullSweepDue(daysAgo(91), TODAY, "cam4024", 90, 0));
		assertTrue(RetrievalWindow.fullSweepDue(daysAgo(90), TODAY, "cam4024", 90, 0));
	}

	@Test
	public void fullSweepNotDueBelowMaxAge() {
		assertFalse(RetrievalWindow.fullSweepDue(daysAgo(89), TODAY, "cam4024", 90, 0));
		assertFalse(RetrievalWindow.fullSweepDue(daysAgo(1), TODAY, "cam4024", 90, 0));
	}

	@Test
	public void zeroOrNegativeMaxAgeNeverEscalates() {
		// The parameterless default: even a uid with no sweep on record must not be
		// upgraded — non-batch callers are not budgeting these.
		assertFalse(RetrievalWindow.fullSweepDue(null, TODAY, "cam4024", 0, 45));
		assertFalse(RetrievalWindow.fullSweepDue(daysAgo(10_000), TODAY, "cam4024", 0, 45));
		assertFalse(RetrievalWindow.fullSweepDue(daysAgo(10_000), TODAY, "cam4024", -1, 45));
	}

	@Test
	public void noSweepOnRecordIsDueWhenEscalationEnabled() {
		assertTrue(RetrievalWindow.fullSweepDue(null, TODAY, "cam4024", 90, 45));
	}

	// ---- Jitter ----

	@Test
	public void jitterOffsetsSpreadAcrossUids() {
		// The point of the jitter is that uids swept on the same date do NOT all come
		// due together. A constant (e.g. always-zero) offset satisfies stability but
		// defeats the spread; this asserts genuine dispersion across a cohort.
		Set<Integer> offsets = new HashSet<>();
		for (int i = 0; i < 200; i++) {
			int offset = RetrievalWindow.jitterOffset("uid" + i, 45);
			assertTrue(offset >= 0 && offset < 45, "offset " + offset + " outside [0,45)");
			offsets.add(offset);
		}
		assertTrue(offsets.size() > 10, "expected a spread of offsets, got " + offsets.size() + " distinct value(s)");
	}

	@Test
	public void jitterOffsetIsStableAcrossDistinctStringInstances() {
		// Stability must come from the uid's value, not object identity or a RNG —
		// otherwise the spread re-randomizes nightly and people flap in and out of
		// dueness. Distinct String instances rule out identity-hash implementations.
		String literal = "cam4024";
		String constructed = new StringBuilder("cam").append("4024").toString();
		assertNotSame(literal, constructed);
		assertEquals(RetrievalWindow.jitterOffset(literal, 45), RetrievalWindow.jitterOffset(constructed, 45));
	}

	@Test
	public void jitterShiftsTheDueBoundaryPerUid() {
		// Find two uids with different offsets, then show each uid's due boundary sits
		// exactly at maxAge + its own offset: due on the boundary day, not due one day
		// fresher. This fails for any implementation that ignores the uid.
		String uidA = "uid0";
		String uidB = null;
		for (int i = 1; i < 1000 && uidB == null; i++) {
			if (RetrievalWindow.jitterOffset("uid" + i, 45) != RetrievalWindow.jitterOffset(uidA, 45)) {
				uidB = "uid" + i;
			}
		}
		assertNotNull(uidB, "expected two uids with different jitter offsets");
		for (String uid : Arrays.asList(uidA, uidB)) {
			int offset = RetrievalWindow.jitterOffset(uid, 45);
			assertTrue(RetrievalWindow.fullSweepDue(daysAgo(90 + offset), TODAY, uid, 90, 45),
					uid + " (offset " + offset + ") must be due at its jittered boundary");
			assertFalse(RetrievalWindow.fullSweepDue(daysAgo(90 + offset - 1), TODAY, uid, 90, 45),
					uid + " (offset " + offset + ") must not be due one day before its jittered boundary");
		}
	}

	@Test
	public void zeroJitterMeansUnshiftedBoundary() {
		assertEquals(0, RetrievalWindow.jitterOffset("cam4024", 0));
		assertEquals(0, RetrievalWindow.jitterOffset(null, 45));
	}

	// ---- Lazy seeding: inference fallback for un-stamped items ----

	@Test
	public void inferredLastFullSweepPicksTheLatestAllPublicationsEntry() {
		ESearchResult result = new ESearchResult();
		result.setESearchPmids(Arrays.asList(
				entry("EmailRetrievalStrategy", on(2026, 3, 1), ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS),
				entry("FullNameRetrievalStrategy", on(2026, 5, 15), ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS),
				// A fresher incremental entry must NOT count as a full sweep.
				entry("GoldStandardRetrievalStrategy", on(2026, 8, 1),
						ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS)));
		assertEquals(on(2026, 5, 15), RetrievalWindow.inferredLastFullSweep(result));
	}

	@Test
	public void inferredLastFullSweepIsNullWithoutAnyAllPublicationsEntry() {
		ESearchResult result = new ESearchResult();
		result.setESearchPmids(Arrays.asList(
				entry("EmailRetrievalStrategy", on(2026, 8, 1),
						ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS)));
		assertNull(RetrievalWindow.inferredLastFullSweep(result));
	}

	@Test
	public void inferredLastFullSweepToleratesSparseItems() {
		assertNull(RetrievalWindow.inferredLastFullSweep(null));
		assertNull(RetrievalWindow.inferredLastFullSweep(new ESearchResult()));

		ESearchResult sparse = new ESearchResult();
		sparse.setESearchPmids(Arrays.asList(
				null,
				entry("EmailRetrievalStrategy", null, ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS),
				entry("FullNameRetrievalStrategy", on(2026, 5, 15), null)));
		assertNull(RetrievalWindow.inferredLastFullSweep(sparse));
	}

	private static ESearchPmid entry(String strategy, Instant retrievalDate,
			ESearchPmid.RetrievalRefreshFlag lookupType) {
		return new ESearchPmid(Arrays.asList(1L), strategy, retrievalDate, lookupType);
	}
}
