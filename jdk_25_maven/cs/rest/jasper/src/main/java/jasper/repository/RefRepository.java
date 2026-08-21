package jasper.repository;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.domain.Metadata;
import jasper.domain.Ref;
import jasper.domain.RefId;
import jasper.domain.proj.RefUrl;
import jasper.domain.proj.RefView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface RefRepository extends JpaRepository<Ref, RefId>, JpaSpecificationExecutor<Ref>, StreamMixin<RefView>, ModifiedCursor, OriginMixin {

	Optional<Ref> findOneByUrlAndOrigin(String url, String origin);
	void deleteByUrlAndOrigin(String url, String origin);
	boolean existsByUrlAndOrigin(String url, String origin);

	@Modifying
	@Query("""
		UPDATE Ref SET
			title = :title,
			comment = :comment,
			tags = :tags,
			sources = :sources,
			alternateUrls = :alternateUrls,
			plugins = :plugins,
			metadata = :metadata,
			published = :published,
			modified = :modified
		WHERE
			url = :url AND
			origin = :origin AND
			modified = :cursor""")
	int optimisticUpdate(
		Instant cursor,
		String url,
		String origin,
		String title,
		String comment,
		List<String> tags,
		List<String> sources,
		List<String> alternateUrls,
		ObjectNode plugins,
		Metadata metadata,
		Instant published,
		Instant modified);

	@Transactional
	@Modifying
	@Query("""
		UPDATE Ref SET
			title = :title,
			comment = :comment,
			tags = :tags,
			sources = :sources,
			alternateUrls = :alternateUrls,
			plugins = :plugins,
			metadata = jsonb_concat(COALESCE(metadata, cast_to_jsonb('{}')), :partialMetadata),
			published = :published,
			modified = :modified
		WHERE
			url = :url AND
			origin = :origin""")
	int pushAsyncMetadata(
		String url,
		String origin,
		String title,
		String comment,
		List<String> tags,
		List<String> sources,
		List<String> alternateUrls,
		ObjectNode plugins,
		Metadata partialMetadata,
		Instant published,
		Instant modified);

	@Query("""
		SELECT max(r.modified)
		FROM Ref r
		WHERE r.origin = :origin""")
	Instant getCursor(String origin);

	@Query(nativeQuery = true, value = "SELECT DISTINCT origin from ref")
	List<String> origins();

	@Modifying(clearAutomatically = true)
	@Query("""
		DELETE FROM Ref ref
		WHERE ref.origin = :origin
			AND ref.modified <= :olderThan""")
	void deleteByOriginAndModifiedLessThanEqual(String origin, Instant olderThan);

	@Query("""
		FROM Ref ref
		WHERE ref.url = :url
			AND ref.published >= :published
			AND (:origin = '' OR ref.origin = :origin OR ref.origin LIKE concat(:origin, '.%'))""")
	List<Ref> findAllPublishedByUrlAndPublishedGreaterThanEqual(String url, String origin, Instant published);

	@Query("""
		FROM Ref r
		WHERE r.url != :url
			AND r.published <= :published
			AND jsonb_exists(r.sources, :url) = true
			AND (:origin = '' OR r.origin = :origin OR r.origin LIKE concat(:origin, '.%'))""")
	List<Ref> findAllResponsesPublishedBeforeThanEqual(String url, String origin, Instant published);

	@Query("""
		SELECT r.url FROM Ref r
		WHERE r.url != :url
			AND jsonb_exists(r.sources, :url) = true
			AND jsonb_exists(COALESCE(jsonb_object_field(r.metadata, 'expandedTags'), r.tags), :tag) = true
			AND (:origin = '' OR r.origin = :origin OR r.origin LIKE concat(:origin, '.%'))""")
	List<String> findAllResponsesWithTag(String url, String origin, String tag);

	@Query("""
		SELECT r.url FROM Ref r
		WHERE r.url != :url
			AND jsonb_exists(r.sources, :url) = true
			AND jsonb_exists(COALESCE(jsonb_object_field(r.metadata, 'expandedTags'), r.tags), :tag) = false
			AND (:origin = '' OR r.origin = :origin OR r.origin LIKE concat(:origin, '.%'))""")
	List<String> findAllResponsesWithoutTag(String url, String origin, String tag);

	@Modifying
	@Transactional
	@Query("""
		UPDATE Ref r
		SET r.metadata = jsonb_set(
			coalesce(r.metadata, cast_to_jsonb('{}')),
			'{obsolete}',
			CASE
				WHEN r.modified = (
					SELECT MAX(r2.modified)
					FROM Ref r2
					WHERE r2.url = :url
						AND (:rootOrigin = '' OR r2.origin = :rootOrigin OR r2.origin LIKE CONCAT(:rootOrigin, '.%'))
				)
				THEN cast_to_jsonb('false')
				ELSE cast_to_jsonb('true')
			END,
			true
		)
		WHERE r.url = :url
			AND (:rootOrigin = '' OR r.origin = :rootOrigin OR r.origin LIKE CONCAT(:rootOrigin, '.%'))
		""")
	int updateObsolete(String url, String rootOrigin);

	@Query("""
		SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Ref r
		WHERE r.url = :url
			AND r.modified > :newerThan
			AND (:rootOrigin = '' OR r.origin = :rootOrigin OR r.origin LIKE concat(:rootOrigin, '.%'))""")
	boolean newerExists(String url, String rootOrigin, Instant newerThan);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("""
		UPDATE Ref r
		SET r.metadata = jsonb_set(r.metadata, '{regen}', cast_to_jsonb('true'), true)
		WHERE r.metadata IS NOT NULL
			AND NOT jsonb_object_field_text(r.metadata, 'regen') = 'true'
			AND (:origin = '' OR r.origin = :origin OR r.origin LIKE concat(:origin, '.%'))""")
	void dropMetadata(String origin);

	@Query("""
		FROM Ref r
		WHERE (r.metadata IS NULL OR jsonb_exists(r.metadata, 'modified') = false OR jsonb_object_field_text(r.metadata, 'regen') = 'true')
			AND (:origin = '' OR r.origin = :origin OR r.origin LIKE concat(:origin, '.%'))
		ORDER BY r.modified DESC
		FETCH FIRST 1 ROW ONLY""")
	Optional<Ref> getRefBackfill(String origin);

	@Query("""
		SELECT r.url AS url, jsonb_object_field_text(jsonb_object_field(r.plugins, '+plugin/origin'), 'proxy') AS proxy
		FROM Ref r
		WHERE r.origin = :origin
			AND jsonb_exists(COALESCE(jsonb_object_field(r.metadata, 'expandedTags'), r.tags), '+plugin/origin') = true
			AND ((jsonb_object_field_text(jsonb_object_field(r.plugins, '+plugin/origin'), 'local') IS NULL AND '' = :remote)
				OR jsonb_object_field_text(jsonb_object_field(r.plugins, '+plugin/origin'), 'local') = :remote)
		FETCH FIRST 1 ROW ONLY""")
	Optional<RefUrl> originUrl(String origin, String remote);

	@Query("""
		SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Ref r
		WHERE jsonb_object_field_text(jsonb_object_field(r.plugins, '_plugin/cache'), 'id') = :id
			AND COALESCE(jsonb_object_field_text(jsonb_object_field(r.plugins, '_plugin/cache'), 'ban'), '') != 'true'
			AND COALESCE(jsonb_object_field_text(jsonb_object_field(r.plugins, '_plugin/cache'), 'noStore'), '') != 'true'""")
	boolean cacheExists(String id);

}
