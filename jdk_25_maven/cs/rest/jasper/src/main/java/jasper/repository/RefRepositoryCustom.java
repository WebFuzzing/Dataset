package jasper.repository;

import java.util.List;

/**
 * Database-specific plugin tag queries (native SQL with LATERAL/json_each).
 * Implementations are selected via @Profile.
 */
public interface RefRepositoryCustom {
	List<String> findAllPluginTagsInResponses(String url, String origin);
	List<Object[]> countPluginTagsInResponses(String url, String origin);
	List<String> findAllUserPluginTagsInResponses(String url, String origin);
}
