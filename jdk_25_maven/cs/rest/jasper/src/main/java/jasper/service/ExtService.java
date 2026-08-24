package jasper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.Patch;
import io.micrometer.core.annotation.Timed;
import jasper.component.IngestExt;
import jasper.domain.Ext;
import jasper.errors.InvalidPatchException;
import jasper.errors.NotFoundException;
import jasper.repository.ExtRepository;
import jasper.repository.filter.TagFilter;
import jasper.security.Auth;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.ExtDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static jasper.domain.proj.Tag.localTag;
import static jasper.domain.proj.Tag.tagOrigin;
import static jasper.repository.spec.ExtSpec.sort;
import static org.springframework.data.domain.PageRequest.of;

@Service
public class ExtService {
	private static final Logger logger = LoggerFactory.getLogger(ExtService.class);

	@Autowired
	ExtRepository extRepository;

	@Autowired
	IngestExt ingest;

	@Autowired
	Auth auth;

	@Autowired
	DtoMapper mapper;

	@Autowired
	ObjectMapper objectMapper;

	@PreAuthorize("@auth.canCreateTag(#ext.qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public Instant create(Ext ext) {
		ingest.create(ext);
		return ext.getModified();
	}

	@PreAuthorize("@auth.canWriteTag(#ext.qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public void push(Ext ext) {
		ingest.push(auth.getOrigin(), ext, true, false);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadTag(#qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public ExtDto get(String qualifiedTag) {
		return extRepository.findOneByQualifiedTag(qualifiedTag)
			.map(mapper::domainToDto)
			.orElseThrow(() -> new NotFoundException("Ext " + qualifiedTag));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public Instant cursor(String origin) {
		return extRepository.getCursor(origin);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.minRole()")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public Page<ExtDto> page(TagFilter filter, Pageable pageable) {
		return extRepository
			.findAll(
				sort(
					auth.<Ext>tagReadSpec()
						.and(filter.spec()),
					pageable),
				of(pageable.getPageNumber(), pageable.getPageSize()))
			.map(mapper::domainToDto);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.minRole()")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public long count(TagFilter filter) {
		return extRepository
			.count(
				auth.<Ext>tagReadSpec()
					.and(filter.spec()));
	}

	@PreAuthorize("@auth.canWriteTag(#ext.qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public Instant update(Ext ext) {
		ingest.update(ext);
		return ext.getModified();
	}

	@PreAuthorize("@auth.canWriteTag(#qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public Instant patch(String qualifiedTag, Instant cursor, Patch patch) {
		var created = false;
		var ext = extRepository.findOneByQualifiedTag(qualifiedTag).orElse(null);
		if (ext == null) {
			created = true;
			ext = new Ext();
			ext.setTag(localTag(qualifiedTag));
			ext.setOrigin(tagOrigin(qualifiedTag));
		}
		try {
			var patched = patch.apply(objectMapper.convertValue(ext, JsonNode.class));
			var updated = objectMapper.treeToValue(patched, Ext.class);
			if (created) {
				return create(updated);
			} else {
				updated.setModified(cursor);
				return update(updated);
			}
		} catch (JsonPatchException | JsonProcessingException e) {
			throw new InvalidPatchException("Ext " + qualifiedTag, e);
		}
	}

	@Transactional
	@PreAuthorize("@auth.canWriteTag(#qualifiedTag) or @auth.subOrigin(#qualifiedTag) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "ext"}, histogram = true)
	public void delete(String qualifiedTag) {
		try {
			ingest.delete(qualifiedTag);
		} catch (EmptyResultDataAccessException e) {
			// Delete is idempotent
		}
	}
}
