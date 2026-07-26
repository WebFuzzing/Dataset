package jasper.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jasper.domain.proj.HasOrigin;
import jasper.domain.proj.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.compress.utils.Sets;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static jasper.security.AuthoritiesConstants.ADMIN;
import static jasper.security.AuthoritiesConstants.ANONYMOUS;
import static jasper.security.AuthoritiesConstants.BANNED;
import static jasper.security.AuthoritiesConstants.EDITOR;
import static jasper.security.AuthoritiesConstants.MOD;
import static jasper.security.AuthoritiesConstants.USER;
import static jasper.security.AuthoritiesConstants.VIEWER;
import static java.util.Optional.*;
import static java.util.stream.Stream.concat;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Entity
@Getter
@Setter
@IdClass(TagId.class)
@Table(name = "users")
public class User implements Tag {
	public static final String REGEX = "[_+]user(?:/[a-z0-9]+(?:[./][a-z0-9]+)*)?";
	public static final String ROLE_REGEX = "\\w*";
	public static final String QTAG_REGEX = REGEX + HasOrigin.REGEX;
	public static final int NAME_LEN = 512;
	public static final int ROLE_LEN = 32;
	public static final int AUTHORIZED_KEYS_LEN = 65000;

	/**
	 * Valid roles for the User entities.
	 */
	public static final Set<String> ROLES = Sets.newHashSet(ADMIN, MOD, EDITOR, USER, VIEWER, BANNED);

	@Id
	@Column(updatable = false)
	@NotBlank
	@Pattern(regexp = REGEX)
	@Length(max = TAG_LEN)
	private String tag;

	@Id
	@Column(updatable = false)
	@Pattern(regexp = HasOrigin.REGEX)
	@Length(max = ORIGIN_LEN)
	private String origin = "";

	@Length(max = ROLE_LEN)
	@Pattern(regexp = ROLE_REGEX)
	private String role = "";

	@Formula("tag || origin")
	@Setter(AccessLevel.NONE)
	private String qualifiedTag;

	@Length(max = NAME_LEN)
	private String name;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<@NotBlank @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String> readAccess;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<@NotBlank @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String> writeAccess;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<@NotBlank @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String> tagReadAccess;

	@JdbcTypeCode(SqlTypes.JSON)
	private List<@NotBlank @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String> tagWriteAccess;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant modified = Instant.now();

	private byte[] key;

	private byte[] pubKey;

	@Size(max = AUTHORIZED_KEYS_LEN)
	private String authorizedKeys;

	@JdbcTypeCode(SqlTypes.JSON)
	private External external;

	@JsonIgnore
	public String getQualifiedTag() {
		return getTag() + getOrigin();
	}

	@JsonIgnore
	public User addReadAccess(List<String> toAdd) {
		if (toAdd == null) return this;
		if (readAccess == null) {
			readAccess = toAdd;
		} else {
			for (var t : toAdd) {
				if (!readAccess.contains(t)) {
					readAccess.add(t);
				}
			}
		}
		return this;
	}

	@JsonIgnore
	public User addWriteAccess(List<String> toAdd) {
		if (toAdd == null) return this;
		if (writeAccess == null) {
			writeAccess = toAdd;
		} else {
			for (var t : toAdd) {
				if (!writeAccess.contains(t)) {
					writeAccess.add(t);
				}
			}
		}
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return tag.equals(user.tag) && origin.equals(user.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(tag, origin);
	}

	public static boolean isUser(String t) {
		return t.startsWith("+user") ||
			t.startsWith("_user") ||
			t.startsWith("+user/") ||
			t.startsWith("_user/");
	}

	@JsonIgnore
	public boolean hasExternalId() {
		if (external == null) return false;
		if (external.getIds() == null) return false;
		return !external.getIds().isEmpty();
	}

	@JsonIgnore
	public boolean hasExternalId(String id) {
		if (external == null) return false;
		if (external.getIds() == null) return false;
		return external.getIds().contains(id);
	}

	public static Optional<User> merge(List<User> users) {
		if (users.isEmpty()) return empty();
		var result = new User();
		result.setTag(users.getFirst().getTag());
		result.setOrigin(users.getFirst().getOrigin());
		result.setName(users.getFirst().getName());
		result.setKey(users.getFirst().getKey());
		result.setPubKey(users.getFirst().getPubKey());
		result.setAuthorizedKeys(users.getFirst().getAuthorizedKeys());
		result.setExternal(users.getFirst().getExternal());
		result.setRole(users.stream().map(User::getRole).max((a, b) -> {
			if (ADMIN.equals(a)) return 1;
			if (ADMIN.equals(b)) return -1;
			if (MOD.equals(a)) return 1;
			if (MOD.equals(b)) return -1;
			if (EDITOR.equals(a)) return 1;
			if (EDITOR.equals(b)) return -1;
			if (USER.equals(a)) return 1;
			if (USER.equals(b)) return -1;
			if (VIEWER.equals(a)) return 1;
			if (VIEWER.equals(b)) return -1;
			return 0;
		}).orElse(ANONYMOUS));
		result.setReadAccess(users.stream().flatMap(u -> emptyIfNull(u.getReadAccess()).stream()).distinct().toList());
		result.setWriteAccess(concat(
			users.stream().skip(1).map(User::getTag),
			users.stream().flatMap(u -> emptyIfNull(u.getWriteAccess()).stream())
		).distinct().toList());
		result.setTagReadAccess(users.stream().flatMap(u -> emptyIfNull(u.getTagReadAccess()).stream()).distinct().toList());
		result.setTagWriteAccess(users.stream().flatMap(u -> emptyIfNull(u.getTagWriteAccess()).stream()).distinct().toList());
		return of(result);
	}
}
