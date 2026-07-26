package jasper.service.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jasper.component.Storage;
import jasper.domain.Ext;
import jasper.domain.External;
import jasper.domain.Metadata;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.domain.proj.HasTags;
import jasper.security.Auth;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jasper.domain.proj.Tag.publicTag;

/**
 * Filtering mapper. Removes fields hidden to the user.
 */
@Mapper(componentModel = "spring")
public abstract class DtoMapper {

	@Autowired
	Auth auth;

	@Autowired
	ObjectMapper objectMapper;

	@Mapping(target = "metadata.userUrls", ignore = true)
	public abstract RefDto domainToDto(Ref ref);

	@Mapping(target = "responses", source = "metadata.responses")
	@Mapping(target = "metadata.userUrls", ignore = true)
	public abstract RefNodeDto domainToNodeDto(Ref ref);

	public abstract RefReplDto dtoToRepl(RefDto ref);

	public abstract ExtDto domainToDto(Ext ext);

	public abstract UserDto domainToDto(User user);
	public abstract ExternalDto domainToDto(External external);

	public abstract PluginDto domainToDto(Plugin plugin);

	public abstract TemplateDto domainToDto(Template plugin);

	public abstract BackupDto domainToDto(Storage.StorageRef plugin);

	@AfterMapping
	protected void filterTags(@MappingTarget HasTags ref) {
		if (ref.getTags() == null) return;
		ref.setTags(new ArrayList<>(auth.filterTags(ref.getTags())));
	}

	@AfterMapping
	protected void filterPlugins(@MappingTarget HasTags ref) {
		if (ref.getPlugins() == null) return;
		var filteredPlugins = objectMapper.createObjectNode();
		ref.getPlugins().fieldNames().forEachRemaining(tag -> {
			if (auth.canReadTag(tag + auth.getOrigin())) filteredPlugins.set(tag, ref.getPlugins().get(tag));
		});
		if (filteredPlugins.isEmpty()) {
			ref.setPlugins(null);
		} else {
			ref.setPlugins(filteredPlugins);
		}
	}

	@AfterMapping
	protected void filterMetadata(@MappingTarget MetadataDto metadata) {
		if (metadata.getPlugins() == null) return;
		var filteredPlugins = new HashMap<String, Integer>();
		metadata.getPlugins().entrySet().iterator().forEachRemaining(e -> {
			if (auth.canReadTag(e.getKey() + auth.getOrigin())) {
				filteredPlugins.put(e.getKey(), e.getValue());
			}
		});
		if (filteredPlugins.isEmpty()) {
			metadata.setPlugins(null);
		} else {
			metadata.setPlugins(filteredPlugins);
		}
	}

	@AfterMapping
	protected void filterTags(@MappingTarget UserDto userDto) {
		userDto.setReadAccess(auth.filterTags(userDto.getReadAccess()));
		userDto.setWriteAccess(auth.filterTags(userDto.getWriteAccess()));
	}

	@AfterMapping
	protected void userUrlsMetadata(Metadata source, @MappingTarget MetadataDto target) {
		if (source.getUserUrls() == null) return;
		if (auth.getUserTag() == null) return;
		var prefix = "tag:/" + publicTag(auth.getUserTag().tag) + "?url=";
		target.setUserUrls(source.getUserUrls().entrySet().stream()
			// TODO: how is null getting in here
			.filter(e -> e.getValue().stream().anyMatch(url -> url != null && url.startsWith(prefix)))
			.map(Map.Entry::getKey)
			.toList()
		);
	}

	public int countMetadata(List<String> responses) {
		if (responses == null) return 0;
		return responses.size();
	}
}
