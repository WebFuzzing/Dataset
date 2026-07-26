package jasper.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("sqlite")
public class RefRepositoryImplSqlite implements RefRepositoryCustom {

	@PersistenceContext
	private EntityManager em;

	@Override
	public List<String> findAllPluginTagsInResponses(String url, String origin) {
		return em.createNativeQuery("""
			SELECT DISTINCT j.value AS tag
			FROM ref r, json_each(COALESCE(json_extract(r.metadata, '$.expandedTags'), r.tags)) AS j
			WHERE r.url != :url
				AND EXISTS (SELECT 1 FROM json_each(r.sources) s WHERE s.value = :url)
				AND (j.value LIKE 'plugin/%' OR j.value LIKE '+plugin/%' OR j.value LIKE '\\_plugin/%' ESCAPE '\\' OR j.value = 'plugin' OR j.value = '+plugin' OR j.value = '_plugin')
				AND (:origin = '' OR r.origin = :origin OR r.origin LIKE (:origin || '.%'))
			""", String.class)
			.setParameter("url", url)
			.setParameter("origin", origin)
			.getResultList();
	}

	@Override
	public List<Object[]> countPluginTagsInResponses(String url, String origin) {
		return em.createNativeQuery("""
			SELECT j.value AS tag, COUNT(DISTINCT r.url)
			FROM ref r, json_each(COALESCE(json_extract(r.metadata, '$.expandedTags'), r.tags)) AS j
			WHERE r.url != :url
				AND EXISTS (SELECT 1 FROM json_each(r.sources) s WHERE s.value = :url)
				AND (j.value LIKE 'plugin/%' OR j.value LIKE '+plugin/%' OR j.value LIKE '\\_plugin/%' ESCAPE '\\' OR j.value = 'plugin' OR j.value = '+plugin' OR j.value = '_plugin')
				AND (:origin = '' OR r.origin = :origin OR r.origin LIKE (:origin || '.%'))
			GROUP BY j.value
			""", Object[].class)
			.setParameter("url", url)
			.setParameter("origin", origin)
			.getResultList();
	}

	@Override
	public List<String> findAllUserPluginTagsInResponses(String url, String origin) {
		return em.createNativeQuery("""
			SELECT DISTINCT j.value AS tag
			FROM ref r, json_each(COALESCE(json_extract(r.metadata, '$.expandedTags'), r.tags)) AS j
			WHERE r.url != :url
				AND EXISTS (SELECT 1 FROM json_each(r.sources) s WHERE s.value = :url)
				AND (j.value LIKE 'plugin/user/%' OR j.value LIKE '+plugin/user/%' OR j.value LIKE '\\_plugin/user/%' ESCAPE '\\' OR j.value = 'plugin/user' OR j.value = '+plugin/user' OR j.value = '_plugin/user')
				AND r.origin = :origin
			""", String.class)
			.setParameter("url", url)
			.setParameter("origin", origin)
			.getResultList();
	}
}
