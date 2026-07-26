package jasper.security;

import jakarta.annotation.PostConstruct;
import jasper.component.ConfigCache;
import jasper.config.Config.SecurityConfig;
import jasper.config.Props;
import jasper.config.WebSocketConfig;
import jasper.domain.Ref;
import jasper.domain.User;
import jasper.domain.proj.HasOrigin;
import jasper.domain.proj.HasTags;
import jasper.domain.proj.Tag;
import jasper.errors.FreshLoginException;
import jasper.repository.RefRepository;
import jasper.repository.filter.Query;
import jasper.repository.spec.QualifiedTag;
import jasper.security.jwt.Claims;
import jasper.security.jwt.JwtAuthentication;
import jasper.service.dto.UserDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jasper.config.JacksonConfiguration.dump;
import static jasper.domain.proj.HasOrigin.isSubOrigin;
import static jasper.domain.proj.Tag.matchesTag;
import static jasper.domain.proj.Tag.matchesTemplate;
import static jasper.domain.proj.Tag.tagOrigin;
import static jasper.domain.proj.Tag.tagUrl;
import static jasper.domain.proj.Tag.urlToTag;
import static jasper.domain.proj.Tag.userUrl;
import static jasper.repository.spec.OriginSpec.isOrigin;
import static jasper.repository.spec.QualifiedTag.qt;
import static jasper.repository.spec.QualifiedTag.qtList;
import static jasper.repository.spec.QualifiedTag.selector;
import static jasper.repository.spec.QualifiedTag.selectors;
import static jasper.repository.spec.RefSpec.hasAnyQualifiedTag;
import static jasper.repository.spec.TagSpec.isAnyQualifiedTag;
import static jasper.repository.spec.TagSpec.notPrivateTag;
import static jasper.security.AuthoritiesConstants.ADMIN;
import static jasper.security.AuthoritiesConstants.BANNED;
import static jasper.security.AuthoritiesConstants.EDITOR;
import static jasper.security.AuthoritiesConstants.MOD;
import static jasper.security.AuthoritiesConstants.ROLE_PREFIX;
import static jasper.security.AuthoritiesConstants.USER;
import static jasper.security.AuthoritiesConstants.VIEWER;
import static java.net.URLDecoder.decode;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.jpa.domain.Specification.where;
import static org.springframework.security.core.authority.AuthorityUtils.authorityListToSet;

/**
 * This single class is where all authorization decisions are made.
 * Authorization decisions are made based on six criteria:
 * 1. The user tag
 * 2. The local origin (always the same as the user tag origin)
 * 3. The user role (ADMIN, MOD, EDITOR, USER, VIEWER, ANONYMOUS)
 * 4. The user access tags
 * 5. Is the JWT token fresh? (less than 15 minutes old)
 *
 * These criteria may be sourced in three cascading steps:
 * 1. Application properties (set by command line, environment variables, or default value)
 * 2. {@link SecurityConfig}
 * 3. JWT token claims
 * 4. Request headers
 *
 * The local origin is set by headers with the highest precedence, then JWT, then
 * installed configs and finally application properties.
 * Roles and access tags merge to be the more elevated role.
 *
 * The application properties that can be configured are:
 * 1. localOrigin (""): set the local origin
 * 2. minRole ("ROLE_ANONYMOUS"): set the minimum role
 * 3. defaultRole ("ROLE_ANONYMOUS"): set the default role
 * 4. defaultReadAccess ([]): set the default read access tags
 * 5. defaultWriteAccess ([]): set the default write access tags
 * 6. defaultTagReadAccess ([]): set the default tag read access tags
 * 7. defaultTagWriteAccess ([]): set the default tag write access tags
 * 8. allowLocalOriginHeader (false): enable setting the local origin in the header
 * 9. allowUserTagHeader (false): enable setting the user tag in the header
 * 10. allowUserRoleHeader (false): enable setting the user role in the header
 * 11. allowAuthHeaders (false): enable setting the user access tags in the header
 *
 * The {@link SecurityConfig} fields that can be configured are:
 * 1. minRole ("ROLE_ANONYMOUS"): set the minimum role
 * 2. defaultRole ("ROLE_ANONYMOUS"): set the default role
 * 3. defaultUser (""): set the default user if logged out
 * 4. defaultReadAccess ([]): set the default read access tags
 * 5. defaultWriteAccess ([]): set the default write access tags
 * 6. defaultTagReadAccess ([]): set the default tag read access tags
 * 7. defaultTagWriteAccess ([]): set the default tag write access tags
 *
 * As well {@link SecurityConfig} has other fields for setting the token
 * claim names.
 * When merging values between application properties and
 * {@link SecurityConfig}, the effect is additive. So for minRole, the
 * effective min role is the lower of the two. For defaultRole, both
 * roles are added, so effectively the larger of the two. For all the default
 * access fields, the tags are all combined.
 *
 * The following headers are checked if enabled:
 * 1. Local-Origin
 * 2. User-Tag
 * 3. User-Role
 * 4. Write-Access
 * 5. Read-Access
 * 6. Tag-Write-Access
 * 7. Tag-Read-Access
 *
 * If no username is not set and the role is at least MOD it will default to +user.
 * Otherwise the username will remain unset.
 */
@Component
@RequestScope
public class Auth {
	private static final Logger logger = LoggerFactory.getLogger(Auth.class);

	public static final String USER_TAG_HEADER = "User-Tag";
	public static final String USER_ROLE_HEADER = "User-Role";
	public static final String LOCAL_ORIGIN_HEADER = "Local-Origin";
	public static final String WRITE_ACCESS_HEADER = "Write-Access";
	public static final String READ_ACCESS_HEADER = "Read-Access";
	public static final String TAG_WRITE_ACCESS_HEADER = "Tag-Write-Access";
	public static final String TAG_READ_ACCESS_HEADER = "Tag-Read-Access";

	Props props;
	RoleHierarchy roleHierarchy;
	ConfigCache configs;
	RefRepository refRepository;

	// Cache
	protected Authentication authentication;
	protected Set<String> roles;
	protected Claims claims;
	protected String principal;
	protected QualifiedTag userTag;
	protected String origin;
	protected Optional<User> user;
	protected List<QualifiedTag> readAccess;
	protected List<QualifiedTag> writeAccess;
	protected List<QualifiedTag> tagReadAccess;
	protected List<QualifiedTag> tagWriteAccess;

	public Auth(Props props, RoleHierarchy roleHierarchy, ConfigCache configs, RefRepository refRepository) {
		this.props = props;
		this.roleHierarchy = roleHierarchy;
		this.configs = configs;
		this.refRepository = refRepository;
	}

	public void clear(Authentication authentication) {
		logger.debug("CLEAR AUTHENTICATION {}", authentication.getPrincipal());
		this.authentication = authentication;
		roles = null;
		claims = null;
		principal = null;
		userTag = null;
		user = null;
		readAccess = null;
		writeAccess = null;
		tagReadAccess = null;
		tagWriteAccess = null;
		if (getPrincipal().startsWith("@")) {
			origin = getPrincipal();
		} else {
			origin = qt(getPrincipal()).origin;
		}
	}

	@PostConstruct
	public void log() {
		logger.debug("AUTH{} User: {} {} (hasUser: {})",
			getOrigin(), getPrincipal(), getAuthoritySet(), getUser().isPresent());
		if (logger.isTraceEnabled()) {
			logger.trace("Auth Config: {} {}", dump(configs.root()), dump(security()));
		}
	}

	public SecurityConfig security() {
		return configs.security(getOrigin());
	}

	/**
	 * Is this origin "".
	 */
	public boolean root() {
		return isBlank(getOrigin());
	}

	/**
	 * Is this origin local. Nulls and empty strings are both considered to
	 * be the default origin.
	 */
	public boolean local(String origin) {
		if (isBlank(origin)) return isBlank(getOrigin());
		if (!origin.startsWith("@")) origin = qt(origin).origin;
		return getOrigin().equals(origin);
	}

	/**
	 * Is this origin a sub-origin.
	 */
	public boolean subOrigin(String origin) {
		return isSubOrigin(getOrigin(), tagOrigin(origin));
	}

	/**
	 * Has the user logged in within 15 minutes?
	 */
	public boolean freshLogin() {
		var iat = getClaims().getIssuedAt();
		if (iat != null && iat.isAfter(Instant.now().minus(Duration.of(15, ChronoUnit.MINUTES)))) {
			return true;
		}
		throw new FreshLoginException();
	}

	/**
	 * Is the current user logged in? Only if they have a user tag which is not blank.
	 * Otherwise, an anonymous request is being made and no user tag should be expected.
	 * Mods and Admins cannot make anonymous requests, as they will default to user tag +user.
	 */
	public boolean isLoggedIn() {
		return isNotBlank(getPrincipal()) && !getPrincipal().startsWith("@");
	}

	/**
	 * Can the current user access this origin?
	 */
	public boolean canReadOrigin(String origin) {
		if (!subOrigin(origin)) return false;
		return minRole();
	}

	/**
	 * Can the user read this Ref?
	 * Only considers the Ref given, does not check if it is modified from
	 * the database version.
	 */
	public boolean canReadRef(HasTags ref) {
		// Only origin and sub origins can be read
		if (!subOrigin(ref.getOrigin())) return false;
		// Mods can read anything
		if (hasRole(MOD)) return true;
		// Min Role
		if (!minRole()) return false;
		// User URL
		if (userUrl(ref.getUrl())) return isLoggedIn() && userUrl(ref.getUrl(), getUserTag().tag);
		// Tag URLs
		if (tagUrl(ref.getUrl())) return canReadTag(urlToTag(ref.getUrl()) + ref.getOrigin());
		// No tags, only mods can read
		if (ref.getTags() == null) return false;
		// Add the ref's origin to its tag list
		var qualifiedTags = qtList(ref.getOrigin(), ref.getTags());
		// Check if owner
		if (owns(qualifiedTags)) return true;
		// Check if user read access tags capture anything in the ref tags
		return captures(getReadAccess(), qualifiedTags);
	}

	/**
	 * Can the user read a ref by tags.
	 */
	public boolean canReadRef(String url, String origin) {
		// Only origin and sub origins can be read
		if (!subOrigin(origin)) return false;
		// Mods can read anything
		if (hasRole(MOD)) return true;
		// User URL
		if (userUrl(url) && isLoggedIn() && userUrl(url, getUserTag().tag)) return true;
		// Tag URLs
		if (tagUrl(url)) return canReadTag(urlToTag(url) + origin);
		var maybeExisting = refRepository.findOneByUrlAndOrigin(url, origin);
		return maybeExisting.filter(this::canReadRef).isPresent();
	}

	/**
	 * Can the user update an existing Ref with given updated version?
	 * Checks the existing database version for write access and verifies any
	 * tag additions.
	 * @param ref the updated ref
	 */
	public boolean canWriteRef(Ref ref) {
		// First check if we can write to the existing Ref
		if (!canWriteRef(ref.getUrl(), ref.getOrigin())) return false;
		// If we can write to the existing we are granted permission
		// We do not need to check if we have write access to the updated Ref,
		// as self revocation is allowed
		var maybeExisting = refRepository.findOneByUrlAndOrigin(ref.getUrl(), ref.getOrigin());
		// We do need to check if we are allowed to add any of the new tags
		// by calling canAddTag on each one
		return newTags(ref.getTags(), maybeExisting.map(Ref::getTags)).allMatch(this::canAddTag);
	}

	/**
	 * Can the user write to an existing Ref.
	 */
	public boolean canWriteRef(String url, String origin) {
		// Only writing to the local origin ever permitted
		if (!local(origin)) return false;
		// Min Role
		if (!minRole()) return false;
		// Minimum role for writing
		if (!minWriteRole()) return false;
		// User URL
		if (userUrl(url)) return hasRole(MOD) || isLoggedIn() && userUrl(url, getUserTag().tag);
		// Tag URLs
		if (tagUrl(url)) return hasRole(MOD) || canWriteTag(urlToTag(url) + origin);
		var maybeExisting = refRepository.findOneByUrlAndOrigin(url, origin);
		if (maybeExisting.isEmpty()) {
			// If we're creating, simply having the role USER is enough
			return hasRole(USER);
		}
		var existing = maybeExisting.get();
		// First write check of an existing Ref must be for the locked tag
		if (existing.hasTag("locked")) return false;
		// Mods can write anything in their origin
		if (hasRole(MOD)) return true;
		if (existing.getTags() == null) return false;
		var qualifiedTags = qtList(origin, existing.getTags());
		// Check if owner
		if (owns(qualifiedTags)) return true;
		// Check access tags
		return captures(getWriteAccess(), qualifiedTags);
	}

	/**
	 * Can subscribe to STOMP topic.
	 */
	public boolean canSubscribeTo(String destination) {
		// Min Role
		if (!minRole()) return false;
		if (destination == null) return false;
		if (destination.startsWith("/topic/cursor/")) {
			var origin = destination.substring("/topic/cursor/".length());
			if (origin.equals("default")) origin = "";
			return canReadOrigin(origin);
		} else if (destination.startsWith("/topic/tag/")) {
			var topic = destination.substring("/topic/tag/".length());
			var origin = topic.substring(0, topic.indexOf('/'));
			if (origin.equals("default")) origin = "";
			var tag = topic.substring(topic.indexOf('/') + 1);
			var decodedTag = decode(tag, StandardCharsets.UTF_8);
            return canReadTag(decodedTag + origin);
		} else if (destination.startsWith("/topic/ref/")) {
			var topic = destination.substring("/topic/ref/".length());
			var origin = topic.substring(0, topic.indexOf('/'));
			if (origin.equals("default")) origin = "";
			var url = topic.substring(topic.indexOf('/') + 1);
			var decodedUrl = decode(url, StandardCharsets.UTF_8);
			return canReadRef(decodedUrl, origin);
		} else if (destination.startsWith("/topic/response/")) {
			var topic = destination.substring("/topic/response/".length());
			var origin = topic.substring(0, topic.indexOf('/'));
			if (origin.equals("default")) origin = "";
			return subOrigin(origin);
		} else if (destination.startsWith("/topic/ext/")) {
			var topic = destination.substring("/topic/ext/".length());
			var origin = topic.substring(0, topic.indexOf('/'));
			if (origin.equals("default")) origin = "";
			var tag = topic.substring(topic.indexOf('/') + 1);
			var decodedTag = decode(tag, StandardCharsets.UTF_8);
			return canReadTag(decodedTag + origin);
		}
		return false;
	}

	/**
	 * Does the user have permission to use a tag when tagging Refs?
	 */
	public boolean canAddTag(String tag) {
		// Min Role
		if (!minRole()) return false;
		// Minimum role for writing
		if (!minWriteRole()) return false;
		if (hasRole(MOD)) return true;
		if (isPublicTag(tag)) return true;
		var qt = qt(tag + getOrigin());
		if (isUser(qt)) return true;
		return captures(getTagReadAccess(), qt);
	}

	/**
	 * Does the user have permission to remove a tag when tagging Refs?
	 */
	public boolean canDeleteTag(String tag) {
		// Min Role
		if (!minRole()) return false;
		// Minimum role for writing
		if (!minWriteRole()) return false;
		if (hasRole(MOD)) return true;
		if (!isPrivateTag(tag)) return true;
		var qt = qt(tag + getOrigin());
		if (isUser(qt)) return true;
		return captures(getTagReadAccess(), qt);
	}

	/**
	 * Does the user have permission to use all tags when tagging Refs?
	 */
	public boolean canPatchTags(List<String> tags) {
		// Min Role
		if (!minRole()) return false;
		// Minimum role for writing
		if (!minWriteRole()) return false;
		if (hasRole(MOD)) return true;
		return tags.stream().allMatch(t -> {
			if (t.startsWith("-")) {
				return this.canDeleteTag(t.substring(1));
			} else {
				return this.canAddTag(t);
			}
		});
	}

	/**
	 * Can the user add these tags to an existing ref?
	 */
	public boolean canPatchTags(List<String> tags, String url, String origin) {
		// Only writing to the local origin ever permitted
		if (!local(origin)) return false;
		// Min Role
		if (!minRole()) return false;
		// Minimum role for writing
		if (!minWriteRole()) return false;
		if (hasRole(MOD)) return true;
		for (var tag : tags) {
			if (tag.startsWith("-")) {
				if (!canUntag(tag.substring(1), url, origin)) return false;
			} else {
				if (!canTag(tag, url, origin)) return false;
			}
		}
		return true;
	}

	/**
	 * Can the user add this tag to an existing ref?
	 */
	public boolean canTag(String tag, String url, String origin) {
		// Only writing to the local origin ever permitted
		if (!local(origin)) return false;
		// Min Role
		if (!minRole()) return false;
		if (hasRole(MOD)) return true;
		// Editor has special access to add public tags to Refs they can read
		if (hasRole(EDITOR) &&
			isPublicTag(tag) &&
			// Except for user, an Editor cannot add ownership to a Ref or vice-versa
			!matchesTemplate("user", tag) &&
			// Except for public, an Editor cannot make a private Ref public or vice-versa
			!matchesTag("public", tag) &&
			// Except for locked, an Editor cannot make a locked Ref editable or vice-versa
			!matchesTag("locked", tag) &&
			canReadRef(url, origin)) return true;
		// You can add the tag, and you can edit the ref
		return canAddTag(tag) && canWriteRef(url, origin);
	}

	/**
	 * Can the user remove this tag to an existing ref?
	 */
	public boolean canUntag(String tag, String url, String origin) {
		// Only writing to the local origin ever permitted
		if (!local(origin)) return false;
		// Min Role
		if (!minRole()) return false;
		// Editor has special access to remove public tags to Refs they can read
		if (hasRole(EDITOR) &&
			isPublicTag(tag) &&
			// Except for user, an Editor cannot add ownership to a Ref or vice-versa
			!matchesTemplate("user", tag) &&
			// Except for public, an Editor cannot make a private Ref public or vice-versa
			!matchesTag("public", tag) &&
			// Except for locked, an Editor cannot make a locked Ref editable or vice-versa
			!matchesTag("locked", tag) &&
			canReadRef(url, origin)) return true;
		// You can delete the tag, and you can edit the ref
		return canDeleteTag(tag) && canWriteRef(url, origin);
	}

	/**
	 * Is this a public tag?
	 * Public tags start with a letter or number.
	 */
	public static boolean isPublicTag(String tag) {
		if (isPrivateTag(tag)) return false;
		if (isProtectedTag(tag)) return false;
		return true;
	}

	/**
	 * Is this a private tag?
	 * Private tags start with a _.
	 */
	public static boolean isPrivateTag(String tag) {
		return tag.startsWith("_");
	}

	/**
	 * Is this a protected tag?
	 * Protected tags start with a +.
	 */
	public static boolean isProtectedTag(String tag) {
		return tag.startsWith("+");
	}

	/**
	 * Can the user read the given qualified tag and any associated entities? (Ext, User, Plugin, Template)
	 * A selector is a tag that may contain an origin, or an origin with no tag.
	 */
	public boolean canReadTag(String qualifiedTag) {
		// Min Role
		if (!minRole()) return false;
		// Only origin and sub origins can be read
		if (!subOrigin(selector(qualifiedTag).origin)) return false;
		// The root template is public
		if (isBlank(qualifiedTag) || qualifiedTag.startsWith("@")) return true;
		// All non-private tags can be read
		if (!isPrivateTag(qualifiedTag)) return true;
		// Mod can read anything
		if (hasRole(MOD)) return true;
		// Can read own user tag
		if (isUser(qualifiedTag)) return true;
		// Finally check access tags
		return captures(getTagReadAccess(), qt(qualifiedTag));
	}

	/**
	 * Can the user create the associated Ext entities of a tag?
	 */
	public boolean canCreateTag(String qualifiedTag) {
		if (!canWriteTag(qualifiedTag)) return false;
		// User role is required to create Exts (except your user Ext)
		return hasRole(USER) || hasRole(VIEWER) && isUser(qt(qualifiedTag));
	}

	/**
	 * Can the user modify the associated Ext entities of a tag?
	 */
	public boolean canWriteTag(String qualifiedTag) {
		var qt = qt(qualifiedTag);
		// Only writing to the local origin ever permitted
		if (!local(qt.origin)) return false;
		// Min Role
		if (!minRole()) return false;
		// Minimum role for writing
		if (!minWriteRole()) return false;
		// Viewers may only edit their user ext
		if (hasRole(VIEWER) && isUser(qt)) return true;
		// Mods can write anything in their origin
		if (hasRole(MOD)) return true;
		// Editors have special access to edit public tag Exts
		if (hasRole(EDITOR) && isPublicTag(qualifiedTag)) return true;
		// Check access tags
		return captures(getTagWriteAccess(), qt);
	}

	/**
	 * Does the user's tag match this tag?
	 */
	public boolean isUser(QualifiedTag qt) {
		return !isPublicTag(qt.tag) && isLoggedIn() && getUserTag().matchesDownwards(qt);
	}

	public boolean isUser(String qualifiedTag) {
		return isUser(qt(qualifiedTag));
	}

	public boolean owns(List<QualifiedTag> qt) {
		return qt.stream().anyMatch(this::isUser);
	}

	/**
	 * Check all the individual selectors of the query and verify they can all
	 * be read.
	 */
	public boolean canReadQuery(Query filter) {
		// Min Role
		if (!minRole()) return false;
		// Anyone can read the empty query (retrieve all Refs)
		if (filter.getQuery() == null) return true;
		// Mod
		if (hasRole(MOD)) return true;
		var tagList = Arrays.stream(filter.getQuery().split("[!:|()\\s]+"))
			.filter(StringUtils::isNotBlank)
			.filter(Auth::isPrivateTag)
			.filter(qt -> !isUser(qt))
			.map(QualifiedTag::selector)
			.toList();
		if (tagList.isEmpty()) return true;
		return captures(getTagReadAccess(), tagList);
	}

	/**
	 * Silently remove sorts that reference private plugins the user cannot read.
	 */
	public Pageable pageable(Pageable pageable) {
		if (pageable == null || pageable.getSort().isUnsorted()) return pageable;
		if (hasRole(MOD)) return pageable;
		var orders = pageable.getSort().toList();
		var filtered = orders.stream()
			.filter(order -> {
				var property = order.getProperty();
				String afterPrefix;
				if (property.startsWith("plugins->")) {
					afterPrefix = property.substring("plugins->".length());
				} else if (property.startsWith("metadata->plugins->")) {
					afterPrefix = property.substring("metadata->plugins->".length());
				} else {
					return true;
				}
				int end = afterPrefix.indexOf("->");
				if (end == -1) end = afterPrefix.indexOf(":");
				var tag = end == -1 ? afterPrefix : afterPrefix.substring(0, end);
				if (!isPrivateTag(tag)) return true;
				if (isUser(tag)) return true;
				return captures(getTagReadAccess(), QualifiedTag.selector(tag));
			})
			.toList();
		if (filtered.size() == orders.size()) return pageable;
		return PageRequest.of(
			pageable.getPageNumber(),
			pageable.getPageSize(),
			Sort.by(filtered));
	}

	/**
	 * Has the maximum role or lower.
	 */
	private boolean maxRole(String role) {
		if (props.getMaxRole().equals(role)) return true;
		return roleHierarchy.getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(role)))
			.stream()
			.map(GrantedAuthority::getAuthority)
			.noneMatch(r -> props.getMaxRole().equals(r));
	}

	/**
	 * Has the minimum role or higher.
	 */
	public boolean minRole() {
		// Don't call hasRole() from here or you get an infinite loop
		if (hasAnyRole(BANNED)) return false;
		return (isBlank(props.getMinRole()) || hasAnyRole(props.getMinRole()))
			&& (isBlank(security().getMinRole()) || hasAnyRole(security().getMinRole()));
	}

	/**
	 * Has the minimum role to write.
	 */
	public boolean minWriteRole() {
		if (hasAnyRole(BANNED)) return false;
		return (isBlank(props.getMinWriteRole()) || hasAnyRole(props.getMinWriteRole()))
			&& (isBlank(security().getMinWriteRole()) || hasAnyRole(security().getMinWriteRole()));
	}

	/**
	 * Has the minimum role to configure admin settings.
	 */
	public boolean minConfigRole() {
		if (hasAnyRole(BANNED)) return false;
		if (hasAnyRole(ADMIN)) return true;
		// Lowest valid role for configuring admin settings is EDITOR
		if (!hasAnyRole(EDITOR)) return false;
		return hasAnyRole(props.getMinConfigRole()) && hasAnyRole(security().getMinConfigRole());
	}

	/**
	 * Has the minimum role to fetch external resources.
	 */
	public boolean minFetchRole() {
		if (hasAnyRole(BANNED)) return false;
		if (hasAnyRole(ADMIN)) return true;
		return hasAnyRole(props.getMinFetchRole()) && hasAnyRole(security().getMinFetchRole());
	}

	/**
	 * Has the minimum role download backups.
	 */
	public boolean minReadBackupRole() {
		if (hasAnyRole(BANNED)) return false;
		if (hasAnyRole(ADMIN)) return true;
		// Lowest valid role for reading backups is MOD
		if (!hasAnyRole(MOD)) return false;
		return hasAnyRole(props.getMinReadBackupsRole()) && hasAnyRole(security().getMinReadBackupsRole());
	}

	/**
	 * Can this admin config be edited?
	 */
	public boolean canEditConfig(Tag config) {
		return canEditConfig(config.getQualifiedTag());
	}

	/**
	 * Can this admin config be edited?
	 */
	public boolean canEditConfig(String qualifiedTag) {
		if (!local(qualifiedTag)) return false;
		if (hasAnyRole(ADMIN)) return true;
		if (!minConfigRole()) return false;
		// Non-admins may only edit public configs, or assigned private configs
		return captures(getTagWriteAccess(), qt(qualifiedTag));
	}

	/**
	 * Admin in the root origin.
	 */
	public boolean rootMod() {
		return root() && hasRole(MOD);
	}

	/**
	 * Can this user be updated?
	 * Check if the user can write this tag, and that their role is not smaller.
	 */
	public boolean canWriteUserTag(String qualifiedTag) {
		// Only writing to the local origin ever permitted
		if (!local(qt(qualifiedTag).origin)) return false;
		if (!canWriteTag(qualifiedTag)) return false;
		var role = ofNullable(configs.getUser(qualifiedTag)).map(User::getRole).orElse(null);
		// Only Mods and above can unban
		if (BANNED.equals(role)) return hasRole(MOD);
		// Cannot edit user with higher role
		return isBlank(role) || hasRole(role);
	}

	/**
	 * Can this user be updated with the given user?
	 * Check that the user is writeable, and that the user has write access to all new tags.
	 * Do not allow public tags to be given write access.
	 */
	public boolean canWriteUser(User user) {
		if (!canWriteUserTag(user.getQualifiedTag())) return false;
		// Cannot add role higher than your own
		if (isNotBlank(user.getRole()) && !BANNED.equals(user.getRole()) && !hasRole(user.getRole())) return false;
		// Mods can add any tag permissions
		if (hasRole(MOD)) return true;
		var maybeExisting = ofNullable(configs.getUser(user.getQualifiedTag()));
		// User role is required to create Users
		if (maybeExisting.isEmpty() && !hasRole(USER)) return false;
		// No public tags in write access
		if (user.getWriteAccess() != null && user.getWriteAccess().stream().anyMatch(Auth::isPublicTag)) return false;
		// The writing user must already have write access to give read or write access to another user
		if (!newTags(user.getTagReadAccess(), maybeExisting.map(User::getTagReadAccess)).allMatch(this::tagWriteAccessCaptures)) return false;
		if (!newTags(user.getTagWriteAccess(), maybeExisting.map(User::getTagWriteAccess)).allMatch(this::tagWriteAccessCaptures)) return false;
		if (!newTags(user.getReadAccess(), maybeExisting.map(User::getReadAccess)).allMatch(this::writeAccessCaptures)) return false;
		if (!newTags(user.getWriteAccess(), maybeExisting.map(User::getWriteAccess)).allMatch(this::writeAccessCaptures)) return false;
		return true;
	}

	public UserDto filterUser(UserDto user) {
		if (hasRole(MOD)) return user;
		if (canWriteUserTag(user.getQualifiedTag())) return user;
		user.setExternal(null);
		return user;
	}

	public List<String> filterTags(List<String> tags) {
		if (tags == null) return null;
		if (hasRole(MOD)) return tags;
		return tags.stream()
			.filter(tag -> canReadTag(tag + getOrigin()))
			.toList();
	}

	public List<String> hiddenTags(List<String> tags) {
		if (hasRole(MOD)) return null;
		if (tags == null) return null;
		return tags.stream()
			.filter(tag -> !canReadTag(tag + getOrigin()))
			.toList();
	}

	public List<String> unwritableTags(List<String> tags) {
		if (hasRole(MOD)) return null;
		if (tags == null) return null;
		return tags.stream()
			.filter(tag -> !canAddTag(tag + getOrigin()))
			.toList();
	}

	public Specification<Ref> refReadSpec() {
		var spec = where(hasRole(MOD)
			? isOrigin(getSubOrigins())
			: selector("public" + getSubOrigins()).refSpec());
		if (isLoggedIn()) {
			spec = spec.or(getUserTag().downwardRefSpec());
		}
		return spec.or(hasAnyQualifiedTag(getReadAccess()));
	}

	public <T extends Tag> Specification<T> tagReadSpec() {
		var spec = Specification.<T>where(isOrigin(getSubOrigins()));
		if (!hasRole(MOD)) spec = spec.and(notPrivateTag());
		if (isLoggedIn()) {
			spec = spec.or(getUserTag().downwardSpec());
		}
		return spec.or(isAnyQualifiedTag(getTagReadAccess()));
	}

	protected boolean tagWriteAccessCaptures(String tag) {
		if (hasRole(MOD)) return true;
		return captures(getTagWriteAccess(), qt(tag + getOrigin()));
	}

	protected boolean writeAccessCaptures(String tag) {
		if (hasRole(MOD)) return true;
		return captures(getWriteAccess(), qt(tag + getOrigin()));
	}

	protected static boolean captures(List<QualifiedTag> selectors, List<QualifiedTag> target) {
		if (selectors == null) return false;
		if (selectors.isEmpty()) return false;
		if (target == null) return false;
		if (target.isEmpty()) return false;
		for (var selector : selectors) {
			if (captures(selector, target)) return true;
		}
		return false;
	}

	protected static boolean captures(List<QualifiedTag> selectors, QualifiedTag target) {
		if (selectors == null) return false;
		if (selectors.isEmpty()) return false;
		if (target == null) return false;
		for (var selector : selectors) {
			if (selector.capturesDownwards(target)) return true;
		}
		return false;
	}

	protected static boolean captures(QualifiedTag selector, List<QualifiedTag> target) {
		if (selector == null) return false;
		if (target == null) return false;
		if (target.isEmpty()) return false;
		for (var t : target) {
			if (selector.capturesDownwards(t)) return true;
		}
		return false;
	}

	protected static Stream<String> newTags(List<String> changes, Optional<List<String>> existing) {
		if (changes == null) return Stream.empty();
		if (existing.isEmpty()) return changes.stream();
		return changes.stream().filter(tag -> !existing.get().contains(tag));
	}

	public String getPrincipal() {
		if (principal == null) {
			var authn = getAuthentication();
			if (authn == null) return null;
			if (authn instanceof JwtAuthentication j) {
				principal = j.getPrincipal();
			} else {
				if (authn instanceof AnonymousAuthenticationToken) return null;
				if (authn.getPrincipal() == null) return null;
				if (authn.getPrincipal() instanceof String username) {
					principal = username;
				} else if (authn.getPrincipal() instanceof UserDetails d) {
					principal = d.getUsername();
				} else {
					return null;
				}
			}
		}
		return principal;
	}

	public QualifiedTag getUserTag() {
		if (userTag == null) {
			if (!isLoggedIn()) return null;
			userTag = qt(getPrincipal());
		}
		return userTag;
	}

	protected Optional<User> getUser() {
		if (user == null) {
			var auth = ofNullable(getAuthentication());
			user = auth.map(a -> a.getDetails() instanceof UserDto
				? (User) a.getDetails()
				: null);
			if (isLoggedIn() && user.isEmpty()) {
				user = ofNullable(configs.getUser(getUserTag().toString()));
			}
		}
		return user;
	}

	public String getOrigin() {
		if (origin == null) {
			origin = props.getOrigin();
			var originHeader = getOriginHeader();
			if (originHeader != null && isSubOrigin(props.getLocalOrigin(), originHeader)) {
				origin = originHeader;
			}
		}
		return origin;
	}

	private static String getOriginHeader() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attribs) {
			var originHeader = attribs.getRequest().getHeader(LOCAL_ORIGIN_HEADER);
			logger.trace("{}: {}", LOCAL_ORIGIN_HEADER, originHeader);
			if (isBlank(originHeader)) return null;
			originHeader = originHeader.toLowerCase();
			if ("default".equals(originHeader)) return "";
			if (originHeader.matches(HasOrigin.REGEX)) return originHeader;
		}
		return null;
	}

	protected String getSubOrigins() {
		return getOrigin().isEmpty() ? "@*" : getOrigin() + ".*";
	}

	public List<QualifiedTag> getReadAccess() {
		if (readAccess == null) {
			readAccess = new ArrayList<>(List.of(selector("public" + getSubOrigins())));
			if (props.getDefaultReadAccess() != null) {
				readAccess.addAll(getQualifiedTags(props.getDefaultReadAccess()));
			}
			if (security().getDefaultReadAccess() != null) {
				readAccess.addAll(getQualifiedTags(security().getDefaultReadAccess()));
			}
			if (props.isAllowAuthHeaders()) {
				readAccess.addAll(getHeaderQualifiedTags(READ_ACCESS_HEADER));
			}
			readAccess.addAll(getClaimQualifiedTags(security().getReadAccessClaim()));
			if (isLoggedIn()) {
				readAccess.add(getUserTag());
				readAccess.addAll(selectors(getSubOrigins(), getUser()
						.map(User::getReadAccess)
						.orElse(List.of())));
			}
		}
		return readAccess;
	}

	public List<QualifiedTag> getWriteAccess() {
		if (writeAccess == null) {
			writeAccess = new ArrayList<>();
			if (props.getDefaultWriteAccess() != null) {
				writeAccess.addAll(getQualifiedTags(props.getDefaultWriteAccess()));
			}
			if (security().getDefaultWriteAccess() != null) {
				writeAccess.addAll(getQualifiedTags(security().getDefaultWriteAccess()));
			}
			if (props.isAllowAuthHeaders()) {
				writeAccess.addAll(getHeaderQualifiedTags(WRITE_ACCESS_HEADER));
			}
			writeAccess.addAll(getClaimQualifiedTags(security().getWriteAccessClaim()));
			if (isLoggedIn()) {
				writeAccess.addAll(selectors(getSubOrigins(), getUser()
						.map(User::getWriteAccess)
						.orElse(List.of())));
			}
		}
		return writeAccess;
	}

	public List<QualifiedTag> getTagReadAccess() {
		if (tagReadAccess == null) {
			tagReadAccess = new ArrayList<>(getReadAccess());
			if (props.getDefaultTagReadAccess() != null) {
				tagReadAccess.addAll(getQualifiedTags(props.getDefaultTagReadAccess()));
			}
			if (security().getDefaultTagReadAccess() != null) {
				tagReadAccess.addAll(getQualifiedTags(security().getDefaultTagReadAccess()));
			}
			if (props.isAllowAuthHeaders()) {
				tagReadAccess.addAll(getHeaderQualifiedTags(TAG_READ_ACCESS_HEADER));
			}
			tagReadAccess.addAll(getClaimQualifiedTags(security().getTagReadAccessClaim()));
			if (isLoggedIn()) {
				tagReadAccess.addAll(selectors(getSubOrigins(), getUser()
						.map(User::getTagReadAccess)
						.orElse(List.of())));
			}
		}
		return tagReadAccess;
	}

	public List<QualifiedTag> getTagWriteAccess() {
		if (tagWriteAccess == null) {
			tagWriteAccess = new ArrayList<>(getWriteAccess());
			if (props.getDefaultTagWriteAccess() != null) {
				tagWriteAccess.addAll(getQualifiedTags(props.getDefaultTagWriteAccess()));
			}
			if (security().getDefaultTagWriteAccess() != null) {
				tagWriteAccess.addAll(getQualifiedTags(security().getDefaultTagWriteAccess()));
			}
			if (props.isAllowAuthHeaders()) {
				tagWriteAccess.addAll(getHeaderQualifiedTags(TAG_WRITE_ACCESS_HEADER));
			}
			tagWriteAccess.addAll(getClaimQualifiedTags(security().getTagWriteAccessClaim()));
			if (isLoggedIn()) {
				tagWriteAccess.addAll(selectors(getSubOrigins(), getUser()
						.map(User::getTagWriteAccess)
						.orElse(List.of())));
			}
		}
		return tagWriteAccess;
	}

	public boolean hasAuthority(String authority) {
		return hasAnyAuthority(authority);
	}

	public boolean hasAnyAuthority(String... authorities) {
		return hasAnyAuthorityName(null, authorities);
	}

	public boolean hasRole(String role) {
		if (BANNED.equals(role) && hasAnyRole(BANNED)) return true;
		return minRole() && hasAnyRole(role);
	}

	public boolean hasAnyRole(String... roles) {
		return hasAnyAuthorityName(ROLE_PREFIX, roles);
	}

	private boolean hasAnyAuthorityName(String prefix, String... roles) {
		var roleSet = getAuthoritySet();
		for (var role : roles) {
			var defaultedRole = getRoleWithPrefix(prefix, role);
			if (roleSet.contains(defaultedRole)) {
				return true;
			}
		}
		return false;
	}

	public Authentication getAuthentication() {
		if (authentication == null) {
			authentication = SecurityContextHolder.getContext().getAuthentication();
		}
		return authentication;
	}

	public Claims getClaims() {
		if (claims == null) {
			var auth = getAuthentication();
			if (auth instanceof JwtAuthentication j) {
				claims = j.getClaims();
			} else {
				claims = Claims.EMPTY;
			}
		}
		return claims;
	}

	public Set<String> getAuthoritySet() {
		if (roles == null) {
			if (getAuthentication() == null) {
				roles = new HashSet<>();
			} else {
				var userAuthorities = getAuthentication().getAuthorities();
				roles = authorityListToSet(roleHierarchy.getReachableGrantedAuthorities(userAuthorities))
					.stream()
					.filter(this::maxRole)
					.collect(Collectors.toSet());
			}
		}
		return roles;
	}

	private static String getRoleWithPrefix(String prefix, String role) {
		if (isBlank(role)) return null;
		if (isBlank(prefix)) return role;
		if (role.startsWith(prefix)) return role;
		return prefix + role;
	}

	private static List<String> getHeaderList(String headerName) {
		var header = getHeader(headerName);
		if (header != null) {
			return List.of(header.split(","));
		}
		return List.of();
	}

	public static String getHeader(String headerName) {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
			return a.getRequest().getHeader(headerName);
		}
		if (RequestContextHolder.getRequestAttributes() instanceof WebSocketConfig.WebSocketRequestAttributes a) {
			return a.getHeader(headerName);
		}
		return null;
	}

	private static List<QualifiedTag> getHeaderQualifiedTags(String headerName) {
		return getHeaderList(headerName).stream().map(QualifiedTag::selector).toList();
	}

	public List<String> getClaimTags(String claim) {
		if (!getClaims().containsKey(claim)) return List.of();
		return List.of(getClaims().get(claim, String.class).split(","));
	}

	public List<QualifiedTag> getClaimQualifiedTags(String claim) {
		return getClaimTags(claim).stream().map(QualifiedTag::selector).toList();
	}

	public static List<QualifiedTag> getQualifiedTags(String[] tags) {
		return Stream.of(tags).map(QualifiedTag::selector).toList();
	}

	public static List<QualifiedTag> getQualifiedTags(List<String> tags) {
		return tags.stream().map(QualifiedTag::selector).toList();
	}
}
