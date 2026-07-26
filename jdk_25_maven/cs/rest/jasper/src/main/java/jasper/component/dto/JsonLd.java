package jasper.component.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JsonLd {
	@JsonProperty("@context")
	private String context;
	@JsonProperty("@type")
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	private List<String> types;
	private JsonNode name;
	private String url;
	private JsonNode logo;
	private String description;
	private String embedUrl;
	private String contentUrl;
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	private List<String> thumbnailUrls;
	private String datePublished;
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	private List<JsonNode> image;
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	private List<JsonNode> video;
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	private List<JsonNode> author;
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	private List<JsonNode> publisher;

	public String getType() {
		if (types == null) return null;
		return types.get(0);
	}

	public String getThumbnailUrl() {
		if (thumbnailUrls == null) return null;
		return thumbnailUrls.get(thumbnailUrls.size()-1);
	}
}
