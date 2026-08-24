package jasper.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("!sqlite")
public class RefRepositoryImplPostgres implements RefRepositoryCustom {

	@PersistenceContext
	private EntityManager em;

	@Override
	public List<String> findAllPluginTagsInResponses(String url, String origin) {
		return em.createNativeQuery("""
			SELECT DISTINCT t.tag
			FROM ref r
				CROSS JOIN LATERAL jsonb_array_elements_text(COALESCE(r.metadata->'expandedTags', r.tags)) AS t(tag)
			WHERE r.url != :url
				AND jsonb_exists(r.sources, :url)
				AND t.tag ~ '^[_+]?plugin(/|$)'
				AND (:origin = '' OR r.origin = :origin OR r.origin LIKE concat(:origin, '.%'))
			""", String.class)
			.setParameter("url", url)
			.setParameter("origin", origin)
			.getResultList();
	}

	@Override
	public List<Object[]> countPluginTagsInResponses(String url, String origin) {
		return em.createNativeQuery("""
			SELECT t.tag, COUNT(DISTINCT r.url)
			FROM ref r
				CROSS JOIN LATERAL jsonb_array_elements_text(COALESCE(r.metadata->'expandedTags', r.tags)) AS t(tag)
			WHERE r.url != :url
				AND jsonb_exists(r.sources, :url)
				AND t.tag ~ '^[_+]?plugin(/|$)'
				AND (:origin = '' OR r.origin = :origin OR r.origin LIKE concat(:origin, '.%'))
			GROUP BY t.tag
			""", Object[].class)
			.setParameter("url", url)
			.setParameter("origin", origin)
			.getResultList();
	}

	@Override
	public List<String> findAllUserPluginTagsInResponses(String url, String origin) {
		return em.createNativeQuery("""
			SELECT DISTINCT t.tag
			FROM ref r
				CROSS JOIN LATERAL jsonb_array_elements_text(COALESCE(r.metadata->'expandedTags', r.tags)) AS t(tag)
			WHERE r.url != :url
				AND jsonb_exists(r.sources, :url)
				AND t.tag ~ '^[_+]?plugin/user(/|$)'
				AND r.origin = :origin
			""", String.class)
			.setParameter("url", url)
			.setParameter("origin", origin)
			.getResultList();
	}
}
