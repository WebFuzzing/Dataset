package jasper.repository;

/**
 * Database-specific metadata backfill (bulk UPDATE with CTEs/JSON aggregation).
 * Implementations are selected via @Profile.
 */
public interface BackfillRepository {
	int backfillMetadata(String origin, int batchSize);
}
