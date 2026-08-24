package jasper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.Patch;
import io.micrometer.core.annotation.Timed;
import jasper.component.IngestUser;
import jasper.config.Props;
import jasper.domain.User;
import jasper.errors.InvalidPatchException;
import jasper.errors.NotFoundException;
import jasper.repository.UserRepository;
import jasper.repository.filter.TagFilter;
import jasper.security.Auth;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.RolesDto;
import jasper.service.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import static jasper.domain.proj.Tag.localTag;
import static jasper.domain.proj.Tag.tagOrigin;
import static jasper.repository.spec.UserSpec.sort;
import static jasper.security.AuthoritiesConstants.ADMIN;
import static jasper.security.AuthoritiesConstants.BANNED;
import static jasper.security.AuthoritiesConstants.EDITOR;
import static jasper.security.AuthoritiesConstants.MOD;
import static jasper.security.AuthoritiesConstants.USER;
import static jasper.security.AuthoritiesConstants.VIEWER;
import static jasper.util.Crypto.keyPair;
import static jasper.util.Crypto.writeRsaPrivatePem;
import static jasper.util.Crypto.writeSshRsa;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.data.domain.PageRequest.of;

@Service
public class UserService {

	@Autowired
	Props props;

	@Autowired
	UserRepository userRepository;

	@Autowired
	IngestUser ingest;

	@Autowired
	Auth auth;

	@Autowired
	DtoMapper mapper;

	@Autowired
	ObjectMapper objectMapper;

	@PreAuthorize("@auth.canWriteUser(#user)")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public Instant create(User user) {
		ingest.create(user);
		if (!auth.hasRole(MOD)) user.setExternal(null);
		return user.getModified();
	}

	@PreAuthorize("@auth.canWriteUser(#user)")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public void push(User user) {
		if (isBlank(user.getAuthorizedKeys()) && user.getPubKey() != null) {
			user.setAuthorizedKeys(new String(user.getPubKey(), StandardCharsets.UTF_8));
		}
		var maybeExisting = userRepository.findOneByQualifiedTag(user.getQualifiedTag());
		if (maybeExisting.isPresent()) {
			if (user.getKey() == null) user.setKey(maybeExisting.get().getKey());
			if (user.getPubKey() == null) user.setPubKey(maybeExisting.get().getPubKey());
			if (user.getExternal() == null) user.setExternal(maybeExisting.get().getExternal());
		}
		if (!auth.hasRole(MOD)) user.setExternal(null);
		ingest.push(user);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadTag(#qualifiedTag)")
	@Cacheable(value = "user-dto-cache", key = "#qualifiedTag", condition = "@auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public UserDto get(String qualifiedTag) {
		return userRepository.findOneByQualifiedTag(qualifiedTag)
							 .map(mapper::domainToDto)
							 .map(auth::filterUser)
							 .orElseThrow(() -> new NotFoundException("User " + qualifiedTag));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public Instant cursor(String origin) {
		return userRepository.getCursor(origin);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.minRole()")
	@Cacheable(value = "user-dto-page-cache", key = "#filter.cacheKey(#pageable)", condition = "@auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public Page<UserDto> page(TagFilter filter, Pageable pageable) {
		return userRepository
			.findAll(
				sort(
					auth.<User>tagReadSpec()
						.and(filter.spec()),
					pageable),
				of(pageable.getPageNumber(), pageable.getPageSize()))
			.map(mapper::domainToDto)
			.map(auth::filterUser);
	}

	@PreAuthorize("@auth.canWriteUser(#user)")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public Instant update(User user) {
		var maybeExisting = userRepository.findOneByQualifiedTag(user.getQualifiedTag());
		if (maybeExisting.isEmpty()) throw new NotFoundException("User " + user.getQualifiedTag());
		var existing = maybeExisting.get();
		user.addReadAccess(auth.hiddenTags(existing.getReadAccess()));
		user.addWriteAccess(auth.hiddenTags(existing.getWriteAccess()));
		user.setKey(existing.getKey());
		user.setPubKey(existing.getPubKey());
		if (!auth.hasRole(MOD)) user.setExternal(null);
		ingest.update(user);
		return user.getModified();
	}

	@PreAuthorize("@auth.canWriteUserTag(#qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public Instant patch(String qualifiedTag, Instant cursor, Patch patch) {
		var created = false;
		var user = userRepository.findOneByQualifiedTag(qualifiedTag).orElse(null);
		if (user == null) {
			created = true;
			user = new User();
			user.setTag(localTag(qualifiedTag));
			user.setOrigin(tagOrigin(qualifiedTag));
		}
		try {
			var patched = patch.apply(objectMapper.convertValue(user, JsonNode.class));
			var updated = objectMapper.treeToValue(patched, User.class);
			// @PreAuthorize annotations are not triggered for calls within the same class
			if (!auth.canWriteUser(updated)) throw new AccessDeniedException("Can't add new tags");
			if (created) {
				return create(updated);
			} else {
				updated.setModified(cursor);
				return update(updated);
			}
		} catch (JsonPatchException | JsonProcessingException e) {
			throw new InvalidPatchException("User " + qualifiedTag, e);
		}
	}

	@PreAuthorize("@auth.canWriteUserTag(#qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public Instant keygen(String qualifiedTag) throws NoSuchAlgorithmException, IOException {
		var maybeExisting = userRepository.findOneByQualifiedTag(qualifiedTag);
		if (maybeExisting.isEmpty()) throw new NotFoundException("User " + qualifiedTag);
		var user = maybeExisting.get();
		var kp = keyPair();
		user.setKey(writeRsaPrivatePem(kp.getPrivate()).getBytes());
		user.setPubKey(writeSshRsa(((RSAPublicKey) kp.getPublic()), user.getQualifiedTag()).getBytes());
		ingest.update(user);
		return user.getModified();
	}

	@Transactional
	@PreAuthorize("@auth.canWriteUserTag(#qualifiedTag) or @auth.subOrigin(#qualifiedTag) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public void delete(String qualifiedTag) {
		try {
			ingest.delete(qualifiedTag);
		} catch (EmptyResultDataAccessException e) {
			// Delete is idempotent
		}
	}

	@Timed(value = "jasper.service", extraTags = {"service", "user"}, histogram = true)
	public RolesDto whoAmI() {
		return RolesDto
			.builder()
			.debug(props.isDebug())
			.tag(auth.isLoggedIn() ? auth.getUserTag().toString() : auth.getOrigin())
			.admin(auth.hasRole(ADMIN))
			.mod(auth.hasRole(MOD))
			.editor(auth.hasRole(EDITOR))
			.user(auth.hasRole(USER))
			.viewer(auth.hasRole(VIEWER))
			.banned(auth.hasRole(BANNED))
			.build();
	}
}
