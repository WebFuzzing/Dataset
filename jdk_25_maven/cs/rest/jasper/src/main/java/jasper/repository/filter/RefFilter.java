package jasper.repository.filter;

import jasper.domain.Ref;
import jasper.repository.spec.QualifiedTag;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static jasper.repository.spec.OriginSpec.isNesting;
import static jasper.repository.spec.OriginSpec.isOrigin;
import static jasper.repository.spec.OriginSpec.none;
import static jasper.repository.spec.RefSpec.*;
import static jasper.repository.spec.ReplicationSpec.isModifiedAfter;
import static jasper.repository.spec.ReplicationSpec.isModifiedBefore;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.jpa.domain.Specification.not;

@Builder
@Getter
public class RefFilter implements Query {
	public static final String QUERY = Query.REGEX;
	private static final Logger logger = LoggerFactory.getLogger(RefFilter.class);

	private String origin;
	private Integer nesting;
	private String url;
	private Boolean obsolete;
	private String scheme;
	private String query;
	private String noDescendents;
	private String search;
	private String endsTitle;
	private boolean rankedOrder;
	private String sources;
	private String noSources;
	private String responses;
	private String noResponses;
	private List<String> pluginResponse;
	private List<String> noPluginResponse;
	private String user;
	private List<String> userResponse;
	private List<String> noUserResponse;
	private Instant modifiedBefore;
	private Instant modifiedAfter;
	private Instant publishedBefore;
	private Instant publishedAfter;
	private Instant createdBefore;
	private Instant createdAfter;
	private Instant responseBefore;
	private Instant responseAfter;

	public Specification<Ref> spec(QualifiedTag user) {
		if (user != null) this.user = user.tag;
		return spec();
	}

	public Specification<Ref> spec() {
		if ("!@*".equals(query)) return none();
		var result = Specification.<Ref>unrestricted();
		if (origin != null && !origin.equals("@*")) {
			result = result.and(isOrigin(origin));
		}
		if (obsolete != null) {
			if (!obsolete) {
				result = result.and(isNotObsolete());
			} else {
				result = result.and(not(isNotObsolete()));
			}
		}
		if (nesting != null) {
			result = result.and(isNesting(nesting));
		}
		if (isNotBlank(url)) {
			result = result.and(isUrl(url));
		}
		if (isNotBlank(scheme)) {
			result = result.and(isScheme(scheme));
		}
		if (isNotBlank(query)) {
			result = result.and(new TagQuery(query).refSpec());
		}
		if (isNotBlank(noDescendents)) {
			result = result.and(hasNoChildTag(noDescendents));
		}
		if (isNotBlank(search)) {
			result = result.and(isUrl(search).or(fulltextEn(search, rankedOrder)));
		}
		if (isNotBlank(endsTitle)) {
			result = result.and(endsWithTitle(endsTitle));
		}
		if (isNotBlank(sources)) {
			result = result.and(hasResponse(sources)
				.or(hasInternalResponse(sources)));
		}
		if (isNotBlank(noSources)) {
			result = result.and(not(hasResponse(noSources))
				.and(not(hasInternalResponse(noSources))));
		}
		if (isNotBlank(responses)) {
			result = result.and(hasSource(responses));
		}
		if (isNotBlank(noResponses)) {
			result = result.and(not(hasSource(noResponses)));
		}
		if (pluginResponse != null) {
			for (var r : pluginResponse) {
				result = result.and(hasPluginResponses(r));
			}
		}
		if (noPluginResponse != null) {
			for (var nr : noPluginResponse) {
				result = result.and(hasNoPluginResponses(nr));
			}
		}
		if (isNotBlank(user)) {
			if (userResponse != null) {
				for (var r : userResponse) {
					result = result.and(hasPluginResponses(user, r));
				}
			}
			if (noUserResponse != null) {
				for (var nr : noUserResponse) {
					result = result.and(hasNoPluginResponses(user, nr));
				}
			}
		}
		if (modifiedBefore != null) {
			result = result.and(isModifiedBefore(modifiedBefore));
		}
		if (modifiedAfter != null) {
			result = result.and(isModifiedAfter(modifiedAfter));
		}
		if (publishedBefore != null) {
			result = result.and(isPublishedBefore(publishedBefore));
		}
		if (publishedAfter != null) {
			result = result.and(isPublishedAfter(publishedAfter));
		}
		if (createdBefore != null) {
			result = result.and(isCreatedBefore(createdBefore));
		}
		if (createdAfter != null) {
			result = result.and(isCreatedAfter(createdAfter));
		}
		if (responseBefore != null) {
			result = result.and(isResponseBefore(responseBefore));
		}
		if (responseAfter != null) {
			result = result.and(isResponseAfter(responseAfter));
		}
		return result;
	}
}
