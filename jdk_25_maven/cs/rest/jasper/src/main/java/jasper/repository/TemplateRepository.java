package jasper.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.domain.TagId;
import jasper.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface TemplateRepository extends JpaRepository<Template, TagId>, QualifiedTagMixin<Template>, StreamMixin<Template>, ModifiedCursor, OriginMixin {

	@Modifying
	@Query("""
		UPDATE Template SET
			name = :name,
			config = :config,
			schema = :schema,
			defaults = :defaults,
			modified = :modified
		WHERE
			tag = :tag AND
			origin = :origin AND
			modified = :cursor""")
	int optimisticUpdate(
		Instant cursor,
		String tag,
		String origin,
		String name,
		JsonNode config,
		ObjectNode schema,
		JsonNode defaults,
		Instant modified);

	@Query("""
		SELECT max(t.modified)
		FROM Template t
		WHERE t.origin = :origin""")
	Instant getCursor(String origin);

	@Query(nativeQuery = true, value = "SELECT DISTINCT origin from template")
	List<String> origins();

	@Modifying(clearAutomatically = true)
	@Query("""
		DELETE FROM Template template
		WHERE template.origin = :origin
			AND template.modified <= :olderThan""")
	void deleteByOriginAndModifiedLessThanEqual(String origin, Instant olderThan);

	@Query("""
		FROM Template AS t
		WHERE t.origin = :origin
			AND t.schema IS NOT NULL
			AND COALESCE(CAST(jsonb_object_field(t.config, 'disabled') as boolean), false) = false
			AND (t.tag = ''
				OR t.tag = :tag
				OR (:tag LIKE '+%' AND concat('+', t.tag) = :tag)
				OR locate(concat(t.tag, '/'), :tag) = 1
				OR (:tag LIKE '\\_%' ESCAPE '\\' AND locate(concat(t.tag, '/'), :tag) = 2)
				OR (:tag LIKE '+%' AND locate(concat(t.tag, '/'), :tag) = 2))
		ORDER BY tag_levels(t.tag) ASC""")
	List<Template> findAllForTagAndOriginWithSchema(String tag, String origin);

	@Query("""
		FROM Template AS t
		WHERE t.origin = :origin
			AND t.defaults IS NOT NULL
			AND COALESCE(CAST(jsonb_object_field(t.config, 'disabled') as boolean), false) = false
			AND (t.tag = ''
				OR t.tag = :tag
				OR (:tag LIKE '+%' AND concat('+', t.tag) = :tag)
				OR locate(concat(t.tag, '/'), :tag) = 1
				OR (:tag LIKE '\\_%' ESCAPE '\\' AND locate(concat(t.tag, '/'), :tag) = 2)
				OR (:tag LIKE '+%' AND locate(concat(t.tag, '/'), :tag) = 2))
		ORDER BY tag_levels(t.tag) ASC""")
	List<Template> findAllForTagAndOriginWithDefaults(String tag, String origin);

	@Query("""
		FROM Template AS t
		WHERE t.origin = :origin
			AND COALESCE(CAST(jsonb_object_field(t.config, 'disabled') as boolean), false) = false
			AND t.tag = :template""")
	Optional<Template> findByTemplateAndOrigin(String template, String origin);
}
