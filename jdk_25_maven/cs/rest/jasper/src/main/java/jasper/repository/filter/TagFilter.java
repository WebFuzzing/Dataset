package jasper.repository.filter;

import jasper.domain.proj.Tag;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static jasper.repository.spec.OriginSpec.isNesting;
import static jasper.repository.spec.OriginSpec.isOrigin;
import static jasper.repository.spec.OriginSpec.none;
import static jasper.repository.spec.ReplicationSpec.isModifiedAfter;
import static jasper.repository.spec.ReplicationSpec.isModifiedBefore;
import static jasper.repository.spec.TagSpec.isLevel;
import static jasper.repository.spec.TagSpec.searchTagOrName;
import static jasper.repository.spec.TagSpec.tagEndsWith;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.jpa.domain.Specification.not;

@Builder
@Getter
public class TagFilter implements Query {
	public static final String QUERY = Query.REGEX;
	private static final Logger logger = LoggerFactory.getLogger(TagFilter.class);

	private String origin;
	private Integer nesting;
	private String query;
	private Integer level;
	private Boolean deleted;
	private String search;
	private Instant modifiedBefore;
	private Instant modifiedAfter;

	public <T extends Tag> Specification<T> spec() {
		if ("!@*".equals(query)) return none();
		var result = Specification.<T>unrestricted();
		if (origin != null && !origin.equals("@*")) {
			result = result.and(isOrigin(origin));
		}
		if (isNotBlank(query) && !query.equals("@*")) {
			result = result.and(new TagQuery(query).spec());
		}
		if (nesting != null) {
			result = result.and(isNesting(nesting));
		}
		if (level != null) {
			result = result.and(isLevel(level));
		}
		if (deleted == null || !deleted) {
			result = result.and(not(tagEndsWith("deleted")));
		}
		if (isNotBlank(search)) {
			result = result.and(searchTagOrName(search));
		}
		if (modifiedBefore != null) {
			result = result.and(isModifiedBefore(modifiedBefore));
		}
		if (modifiedAfter != null) {
			result = result.and(isModifiedAfter(modifiedAfter));
		}
		return result;
	}

	public String cacheKey(Pageable pageable) {
		return
			pageable.getPageNumber() + "_" +
			pageable.getPageSize() + "_" +
			pageable.getSort() + "_" +
			origin + "_" +
			query + "_" +
			deleted + "_" +
			modifiedBefore + "_" +
			modifiedAfter + "_" +
			search;
	}
}
