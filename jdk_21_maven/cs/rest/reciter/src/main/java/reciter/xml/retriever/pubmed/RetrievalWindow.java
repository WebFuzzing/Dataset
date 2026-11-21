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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;

/**
 * Decides how far back an ONLY_NEWLY_ADDED retrieval reaches, and when a uid is
 * overdue for a full ALL_PUBLICATIONS sweep.
 *
 * <p>Background: the incremental window used to be {@code retrievalDate - 1 day}, and
 * nothing ever revisits a window once it has been passed. So any single run that
 * failed to surface an article — a swallowed PubMed failure, a client read timeout,
 * a night the job did not run — dropped that article permanently, until somebody
 * noticed and triggered a manual full sweep. (#691 closed the narrower case where a
 * PubMed call errored and the watermark advanced anyway.)
 *
 * <p>Two bounds make coverage self-healing instead:
 * <ol>
 *   <li>{@link #incrementalStart} floors the lookback, so a missed window degrades
 *       from permanent loss to a bounded delay (#695).</li>
 *   <li>{@link #fullSweepDue} caps how stale a uid's last full sweep may get (#696),
 *       which heals holes older than the floor and picks up articles that only became
 *       attributable after an identity change. It only ever fires when the caller
 *       supplies a positive max age — the batch client sends it per cohort and counts
 *       the escalations against its nightly budget; everyone else gets the floor only.</li>
 * </ol>
 *
 * Pure and side-effect free so the decisions are unit-testable without Spring or AWS.
 */
public final class RetrievalWindow {

	private RetrievalWindow() {
	}

	/**
	 * Start of the incremental {@code [EDAT]}/{@code [DP]} window: the earlier of the
	 * previous run's watermark and the configured floor.
	 *
	 * @param lastRetrieval  ESearchResult.retrievalDate, or null if never run
	 * @param today          current date (injected so this stays testable)
	 * @param lookbackFloorDays minimum days to reach back; &lt;= 0 keeps the legacy
	 *                          watermark-only behaviour
	 */
	public static LocalDate incrementalStart(Instant lastRetrieval, LocalDate today, int lookbackFloorDays) {
		LocalDate watermark = lastRetrieval == null ? null : toUtcDate(lastRetrieval).minusDays(1);
		if (lookbackFloorDays <= 0) {
			// Floor disabled: fall back to the historical watermark-only window.
			return watermark == null ? today.minusDays(1) : watermark;
		}
		LocalDate floor = today.minusDays(lookbackFloorDays);
		if (watermark == null) {
			return floor;
		}
		return watermark.isBefore(floor) ? watermark : floor;
	}

	/**
	 * True when this uid's last full sweep is older than {@code maxAgeDays} plus the
	 * uid's stable {@link #jitterOffset jitter offset}.
	 *
	 * @param lastFullSweep most recent completed ALL_PUBLICATIONS retrieval — the
	 *                      persisted {@code lastFullSweep} stamp, or the
	 *                      {@link #inferredLastFullSweep inferred} value while the
	 *                      stamp is still null; null means no full sweep on record,
	 *                      which counts as due
	 * @param today         current date (injected so this stays testable)
	 * @param maxAgeDays    &lt;= 0 disables escalation entirely — the parameterless
	 *                      default, so Publication Manager and ad-hoc callers are
	 *                      never silently upgraded to an unbounded re-disambiguation
	 * @param jitterDays    width of the per-uid spread; &lt;= 0 means no jitter
	 */
	public static boolean fullSweepDue(Instant lastFullSweep, LocalDate today, String uid, int maxAgeDays,
			int jitterDays) {
		if (maxAgeDays <= 0) {
			return false;
		}
		if (lastFullSweep == null) {
			return true;
		}
		LocalDate due = toUtcDate(lastFullSweep).plusDays((long) maxAgeDays + jitterOffset(uid, jitterDays));
		return !today.isBefore(due);
	}

	/**
	 * Stable per-uid offset in {@code [0, jitterDays)}, added to the max age before the
	 * dueness comparison. Sweep dates are heavily clustered (thousands of people share a
	 * single sweep date), so a bare "older than N days" rule would bring a whole cohort
	 * due on the same night; spreading the due date per person dissolves the clump. The
	 * offset must be stable across runs — derived from the uid, not drawn at random —
	 * or the spread would re-randomize nightly and people would flap in and out of
	 * dueness instead of coming due once.
	 */
	public static int jitterOffset(String uid, int jitterDays) {
		if (jitterDays <= 0) {
			return 0;
		}
		return Math.floorMod(uid == null ? 0 : uid.hashCode(), jitterDays);
	}

	/**
	 * Fallback for items whose persisted {@code lastFullSweep} is still null: the most
	 * recent ALL_PUBLICATIONS retrieval recorded on the item's per-strategy entries, or
	 * null if none. This lazily seeds the escalation decision without a table migration.
	 *
	 * <p>The inference errs in the safe direction only: incremental runs re-stamp these
	 * entries (E13), so a person can look <em>less</em> recently swept than they are —
	 * biasing toward an extra sweep, bounded by the client's budget and self-correcting
	 * once the person's first clean sweep stamps a real timestamp. That is why the
	 * inferred value is used for the decision but never written back: only a genuine
	 * clean sweep may stamp.
	 */
	public static Instant inferredLastFullSweep(ESearchResult eSearchResult) {
		if (eSearchResult == null || eSearchResult.getESearchPmids() == null) {
			return null;
		}
		Instant latest = null;
		for (ESearchPmid entry : eSearchResult.getESearchPmids()) {
			if (entry == null || entry.getRetrievalDate() == null
					|| entry.getLookupType() != ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS) {
				continue;
			}
			if (latest == null || entry.getRetrievalDate().isAfter(latest)) {
				latest = entry.getRetrievalDate();
			}
		}
		return latest;
	}

	private static LocalDate toUtcDate(Instant instant) {
		return instant.atZone(ZoneOffset.UTC).toLocalDate();
	}
}
