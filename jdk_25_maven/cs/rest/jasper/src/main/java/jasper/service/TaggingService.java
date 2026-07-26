package jasper.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.Patch;
import io.micrometer.core.annotation.Timed;
import jasper.component.ConfigCache;
import jasper.component.Ingest;
import jasper.component.Tagger;
import jasper.component.Validate;
import jasper.domain.Plugin;
import jasper.errors.DuplicateTagException;
import jasper.errors.InvalidPatchException;
import jasper.errors.ModifiedException;
import jasper.errors.NotFoundException;
import jasper.repository.RefRepository;
import jasper.security.Auth;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.RefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class TaggingService {
	private static final Logger logger = LoggerFactory.getLogger(TaggingService.class);

	@Autowired
	RefRepository refRepository;

	@Autowired
	ConfigCache configs;

	@Autowired
	Ingest ingest;

	@Autowired
	Tagger tagger;

	@Autowired
	Auth auth;

	@Autowired
	DtoMapper mapper;

	@Autowired
	Validate validate;

	@PreAuthorize("@auth.canTag(#tag, #url, #origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "tag"}, histogram = true)
	public Instant create(String tag, String url, String origin) {
		var maybeRef = refRepository.findOneByUrlAndOrigin(url, origin);
		if (maybeRef.isEmpty()) throw new NotFoundException("Ref " + origin + " " + url);
		var ref = maybeRef.get();
		if (ref.hasTag(tag)) throw new DuplicateTagException(tag);
		ref.addTag(tag);
		ingest.update(auth.getOrigin(), ref);
		return ref.getModified();
	}

	@PreAuthorize("@auth.canUntag(#tag, #url, #origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "tag"}, histogram = true)
	public Instant delete(String tag, String url, String origin) {
		if (tag.equals("locked")) {
			throw new AccessDeniedException("Cannot unlock Ref");
		}
		var maybeRef = refRepository.findOneByUrlAndOrigin(url, origin);
		if (maybeRef.isEmpty()) throw new NotFoundException("Ref " + origin + " " + url);
		var ref = maybeRef.get();
		if (!ref.hasTag(tag)) return ref.getModified();
		if (ref.hasTag("locked") && ref.hasPlugin(tag)) {
			throw new AccessDeniedException("Cannot untag locked Ref with plugin data");
		}
		ref.removeTag(tag);
		ingest.update(auth.getOrigin(), ref);
		return ref.getModified();
	}

	@PreAuthorize("@auth.canPatchTags(#tags, #url, #origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "tag"}, histogram = true)
	public Instant tag(List<String> tags, String url, String origin) {
		if (tags.contains("-locked")) {
			throw new AccessDeniedException("Cannot unlock Ref");
		}
		var maybeRef = refRepository.findOneByUrlAndOrigin(url, origin);
		if (maybeRef.isEmpty()) throw new NotFoundException("Ref " + origin + " " + url);
		var ref = maybeRef.get();
		if (ref.hasTag("locked")) {
			for (var t : tags) {
				if (t.startsWith("-") && ref.hasPlugin(t.substring(1))) {
					throw new AccessDeniedException("Cannot untag locked Ref with plugin data");
				}
			}
		}
		ref.addTags(tags);
		ingest.update(auth.getOrigin(), ref);
		return ref.getModified();
	}

	@PreAuthorize("@auth.isLoggedIn() and @auth.hasRole('USER')")
	@Timed(value = "jasper.service", extraTags = {"service", "tag"}, histogram = true)
	public RefDto getResponse(String url) {
		var ref = tagger.getResponseRef(auth.getUserTag().tag, auth.getOrigin(), url);
		return mapper.domainToDto(ref);
	}

	@PreAuthorize("@auth.isLoggedIn() and @auth.hasRole('USER') and @auth.canAddTag(#tag)")
	@Timed(value = "jasper.service", extraTags = {"service", "tag"}, histogram = true)
	public void createResponse(String tag, String url) {
		var ref = tagger.getResponseRef(auth.getUserTag().tag, auth.getOrigin(), url);
		if (isNotBlank(tag) && !ref.hasTag(tag)) {
			ref.addTag(tag);
			try {
				ingest.updateResponse(auth.getOrigin(), ref);
			} catch (ModifiedException e) {
				// TODO: infinite retrys?
				createResponse(tag, url);
			}
		}
	}

	@PreAuthorize("@auth.isLoggedIn() and @auth.hasRole('USER') and @auth.canAddTag(#tag)")
	@Timed(value = "jasper.service", extraTags = {"service", "tag"}, histogram = true)
	public void deleteResponse(String tag, String url) {
		var ref = tagger.getResponseRef(auth.getUserTag().tag, auth.getOrigin(), url);
		ref.removeTag(tag);
		try {
			ingest.updateResponse(auth.getOrigin(), ref);
		} catch (ModifiedException e) {
			// TODO: infinite retrys?
			deleteResponse(tag, url);
		}
	}

	@PreAuthorize("@auth.isLoggedIn() and @auth.hasRole('USER') and @auth.canPatchTags(#tags)")
	@Timed(value = "jasper.service", extraTags = {"service", "tag"}, histogram = true)
	public void respond(List<String> tags, String url, Patch patch) {
		var ref = tagger.getResponseRef(auth.getUserTag().tag, auth.getOrigin(), url);
		for (var tag : tags) {
			var newTag = !tag.startsWith("-") && !ref.hasTag(tag);
			ref.addTag(tag);
			if (newTag) {
				configs.getPlugin(tag, auth.getOrigin())
					.map(Plugin::getDefaults)
					.ifPresent(defaults -> ref.setPlugin(tag, defaults));
			}
		}
		if (patch != null) {
			try {
				var plugins = (ObjectNode) patch.apply(ref.getPlugins() == null ? validate.pluginDefaults(auth.getOrigin(), ref) : ref.getPlugins());
				ref.addPlugins(ref.getTags(), plugins);
			} catch (JsonPatchException e) {
				throw new InvalidPatchException("Ref " + auth.getOrigin() + " " + url, e);
			}
		}
		try {
			ingest.updateResponse(auth.getOrigin(), ref);
		} catch (ModifiedException e) {
			// TODO: infinite retrys?
			respond(tags, url, patch);
		}
	}
}
