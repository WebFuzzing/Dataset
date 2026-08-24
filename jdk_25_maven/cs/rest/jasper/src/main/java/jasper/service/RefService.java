package jasper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.Patch;
import io.micrometer.core.annotation.Timed;
import jasper.component.ConfigCache;
import jasper.component.Ingest;
import jasper.component.Validate;
import jasper.domain.Ref;
import jasper.errors.InvalidPatchException;
import jasper.errors.MaxSourcesException;
import jasper.errors.NotFoundException;
import jasper.repository.RefRepository;
import jasper.repository.filter.RefFilter;
import jasper.security.Auth;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.RefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import static jasper.component.Meta.expandTags;
import static jasper.repository.spec.OriginSpec.isOrigin;
import static jasper.repository.spec.RefSpec.isNotObsolete;
import static jasper.repository.spec.RefSpec.isUrl;
import static jasper.repository.spec.RefSpec.sort;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.data.domain.PageRequest.ofSize;

@Service
public class RefService {
	private static final Logger logger = LoggerFactory.getLogger(RefService.class);

	@Autowired
	RefRepository refRepository;

	@Autowired
	Ingest ingest;

	@Autowired
	Auth auth;

	@Autowired
	Validate validate;

	@Autowired
	DtoMapper mapper;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ConfigCache configs;

	@PreAuthorize("@auth.canWriteRef(#ref)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public Instant create(Ref ref) {
		var root = configs.root();
		if (ref.getSources() != null && ref.getSources().size() > root.getMaxSources()) {
			throw new MaxSourcesException(root.getMaxSources(), ref.getSources().size());
		}
		ingest.create(auth.getOrigin(), ref);
		return ref.getModified();
	}

	@PreAuthorize("@auth.canWriteRef(#ref)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public void push(Ref ref) {
		var root = configs.root();
		if (ref.getSources() != null && ref.getSources().size() > root.getMaxSources()) {
			logger.warn("Ignoring max count for push. Max count is set to {}. Ref contains {} sources.", root.getMaxSources(), ref.getSources().size());
		}
		ingest.push(auth.getOrigin(), ref, true, false);
	}

	@Transactional(readOnly = true)
	@PostAuthorize("@auth.canReadRef(returnObject)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public RefDto get(String url, String origin) {
		return refRepository.findOneByUrlAndOrigin(url, origin)
			.or(() -> refRepository.findOne(isUrl(url).and(isOrigin(origin))))
			.map(mapper::domainToDto)
			.orElseThrow(() -> new NotFoundException("Ref " + origin + " " + url));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public Instant cursor(String origin) {
		return refRepository.getCursor(origin);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadQuery(#filter)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public Page<RefDto> page(RefFilter filter, Pageable pageable) {
		return refRepository
			.findAll(
				sort(
					auth.refReadSpec()
						.and(filter.spec(auth.getUserTag())),
					auth.pageable(pageable)),
				of(pageable.getPageNumber(), pageable.getPageSize()))
			.map(mapper::domainToDto);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadQuery(#filter)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public long count(RefFilter filter) {
		return refRepository
			.count(
				auth.refReadSpec()
					.and(filter.spec(auth.getUserTag())));
	}

	@PreAuthorize("@auth.canWriteRef(#ref)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public Instant update(Ref ref) {
		var root = configs.root();
		if (ref.getSources() != null && ref.getSources().size() > root.getMaxSources()) {
			throw new MaxSourcesException(root.getMaxSources(), ref.getSources().size());
		}
		var maybeExisting = refRepository.findOneByUrlAndOrigin(ref.getUrl(), ref.getOrigin());
		if (maybeExisting.isEmpty()) throw new NotFoundException("Ref " + ref.getOrigin() + " " + ref.getUrl());
		var existing = maybeExisting.get();
		// Hidden tags cannot be removed
		var hiddenTags = auth.hiddenTags(existing.getExpandedTags());
		ref.addTags(hiddenTags);
		ref.addPlugins(hiddenTags, existing.getPlugins());
		// Unwritable tags may only be removed, plugin data may not be modified
		var unwritableTags = auth.unwritableTags(expandTags(ref.getTags()));
		ref.addPlugins(unwritableTags, existing.getPlugins());
		ingest.update(auth.getOrigin(), ref);
		return ref.getModified();
	}

	@PreAuthorize("@auth.canWriteRef(#url, #origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public Instant patch(String url, String origin, Instant cursor, Patch patch) {
		// TODO: disable patching for large refs
		var created = false;
		var ref = refRepository.findOneByUrlAndOrigin(url, origin).orElse(null);
		if (ref == null) {
			created = true;
			var current = refRepository.findAll(isUrl(url).and(isNotObsolete()), ofSize(1));
			ref = current.isEmpty() ? new Ref() : current.getContent().getFirst();
			ref.setUrl(url);
			ref.setOrigin(origin);
		}
		ref.setPlugins(validate.pluginDefaults(auth.getOrigin(), ref));
		try {
			var patched = patch.apply(objectMapper.convertValue(ref, JsonNode.class));
			var updated = objectMapper.treeToValue(patched, Ref.class);
			if (updated.getTags() != null) {
				// Tolerate duplicate tags
				updated.setTags(new ArrayList<>(new LinkedHashSet<>(updated.getTags())));
			}
			// @PreAuthorize annotations are not triggered for calls within the same class
			if (!auth.canWriteRef(updated)) throw new AccessDeniedException("Can't add new tags");
			if (created) {
				return create(updated);
			} else {
				updated.setModified(cursor);
				return update(updated);
			}
		} catch (JsonPatchException | JsonProcessingException e) {
			throw new InvalidPatchException("Ref " + origin + " " + url, e);
		}
	}

	@Transactional
	@PreAuthorize("@auth.canWriteRef(#url, #origin) or @auth.subOrigin(#origin) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "ref"}, histogram = true)
	public void delete(String url, String origin) {
		try {
			ingest.delete(auth.getOrigin(), url, origin);
		} catch (EmptyResultDataAccessException e) {
			// Delete is idempotent
		}
	}
}
