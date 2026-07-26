package jasper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.Patch;
import io.micrometer.core.annotation.Timed;
import jasper.component.IngestPlugin;
import jasper.domain.Plugin;
import jasper.errors.InvalidPatchException;
import jasper.errors.NotFoundException;
import jasper.repository.PluginRepository;
import jasper.repository.filter.TagFilter;
import jasper.security.Auth;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.PluginDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static jasper.domain.proj.Tag.localTag;
import static jasper.domain.proj.Tag.tagOrigin;
import static jasper.repository.spec.PluginSpec.sort;
import static org.springframework.data.domain.PageRequest.of;

@Service
public class PluginService {

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	IngestPlugin ingest;

	@Autowired
	Auth auth;

	@Autowired
	DtoMapper mapper;

	@Autowired
	ObjectMapper objectMapper;

	@PreAuthorize("@auth.canEditConfig(#plugin)")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public Instant create(Plugin plugin) {
		ingest.create(plugin);
		return plugin.getModified();
	}

	@PreAuthorize("@auth.canEditConfig(#plugin)")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public void push(Plugin plugin) {
		ingest.push(plugin);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadTag(#qualifiedTag)")
	@Cacheable(value = "plugin-dto-cache", key = "#qualifiedTag")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public PluginDto get(String qualifiedTag) {
		return pluginRepository.findOneByQualifiedTag(qualifiedTag)
			.map(mapper::domainToDto)
			.orElseThrow(() -> new NotFoundException("Plugin " + qualifiedTag));
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.canReadOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public Instant cursor(String origin) {
		return pluginRepository.getCursor(origin);
	}

	@Transactional(readOnly = true)
	@PreAuthorize("@auth.minRole()")
	@Cacheable(value = "plugin-dto-page-cache", key = "#filter.cacheKey(#pageable)", condition = "@auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public Page<PluginDto> page(TagFilter filter, Pageable pageable) {
		return pluginRepository
			.findAll(
				sort(
					auth.<Plugin>tagReadSpec()
						.and(filter.spec()),
					pageable),
				of(pageable.getPageNumber(), pageable.getPageSize()))
			.map(mapper::domainToDto);
	}

	@PreAuthorize("@auth.canEditConfig(#plugin)")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public Instant update(Plugin plugin) {
		ingest.update(plugin);
		return plugin.getModified();
	}

	@PreAuthorize("@auth.canEditConfig(#qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public Instant patch(String qualifiedTag, Instant cursor, Patch patch) {
		var created = false;
		var plugin = pluginRepository.findOneByQualifiedTag(qualifiedTag).orElse(null);
		if (plugin == null) {
			created = true;
			plugin = new Plugin();
			plugin.setTag(localTag(qualifiedTag));
			plugin.setOrigin(tagOrigin(qualifiedTag));
		}
		try {
			var patched = patch.apply(objectMapper.convertValue(plugin, JsonNode.class));
			var updated = objectMapper.treeToValue(patched, Plugin.class);
			if (created) {
				return create(updated);
			} else {
				updated.setModified(cursor);
				return update(updated);
			}
		} catch (JsonPatchException | JsonProcessingException e) {
			throw new InvalidPatchException("Plugin " + qualifiedTag, e);
		}
	}

	@Transactional
	@PreAuthorize("@auth.canEditConfig(#qualifiedTag) or @auth.subOrigin(#qualifiedTag) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "plugin"}, histogram = true)
	public void delete(String qualifiedTag) {
		try {
			ingest.delete(qualifiedTag);
		} catch (EmptyResultDataAccessException e) {
			// Delete is idempotent
		}
	}
}
