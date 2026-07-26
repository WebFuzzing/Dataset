package jasper.component.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jasper.domain.Ext;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.service.dto.ExtDto;
import jasper.service.dto.PluginDto;
import jasper.service.dto.RefDto;
import jasper.service.dto.TemplateDto;
import jasper.service.dto.UserDto;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ComponentDtoMapper {

	@Autowired
	ObjectMapper objectMapper;

	@Mapping(target = "metadata.userUrls", ignore = true)
	public abstract RefDto domainToDto(Ref ref);

	public abstract UserDto domainToDto(User ref);

	public abstract ExtDto domainToDto(Ext ref);

	public abstract PluginDto domainToDto(Plugin ref);

	public abstract TemplateDto domainToDto(Template ref);

	public int countMetadata(List<String> responses) {
		if (responses == null) return 0;
		return responses.size();
	}

	public abstract RefUpdateDto dtoToUpdateDto(RefDto ref);

	@AfterMapping
	protected void publicTags(@MappingTarget RefUpdateDto ref) {
		if (ref.getTags() == null) return;
		var filtered = new ArrayList<>(ref.getTags().stream().filter(t -> !t.startsWith("_")).toList());
		if (ref.getPlugins() != null && filtered.size() < ref.getTags().size()) {
			var filteredPlugins = objectMapper.createObjectNode();
			ref.getPlugins().fieldNames().forEachRemaining(field -> {
				if (filtered.contains(field)) filteredPlugins.set(field, ref.getPlugins().get(field));
			});
			if (filteredPlugins.isEmpty()) {
				ref.setPlugins(null);
			} else {
				ref.setPlugins(filteredPlugins);
			}
		}
		ref.setTags(filtered);
	}

	@AfterMapping
	protected void publicMetadata(@MappingTarget MetadataUpdateDto metadata) {
		if (metadata.getPlugins() == null) return;
		var filteredPlugins = new HashMap<String, Integer>();
		metadata.getPlugins().entrySet().iterator().forEachRemaining(e -> {
			if (!e.getKey().startsWith("_")) {
				filteredPlugins.put(e.getKey(), e.getValue());
			}
		});
		if (filteredPlugins.isEmpty()) {
			metadata.setPlugins(null);
		} else {
			metadata.setPlugins(filteredPlugins);
		}
	}
}
