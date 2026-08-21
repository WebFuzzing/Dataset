package jasper.repository;

/**
 * Database-specific index management (GIN indexes on PostgreSQL, no-ops/FTS on SQLite).
 * Implementations are selected via @Profile.
 */
public interface IndexRepository {
	void dropTags();
	void buildTags();
	void dropExpandedTags();
	void buildExpandedTags();
	void dropSources();
	void buildSources();
	void dropAlts();
	void buildAlts();
	void dropFulltext();
	void buildFulltext();
	void dropPublished();
	void buildPublished();
	void dropModified();
	void buildModified();
}
