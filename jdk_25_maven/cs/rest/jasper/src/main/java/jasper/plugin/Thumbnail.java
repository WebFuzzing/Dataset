package jasper.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jasper.domain.Ref;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Thumbnail implements Serializable {
	private String url;
	private String color;
	private String emoji;
	private int radius;

	@JsonIgnore
	public boolean isBlank() {
		return StringUtils.isBlank(url) && StringUtils.isBlank(color) && StringUtils.isBlank(emoji);
	}

	public static Thumbnail getThumbnail(Ref ref) {
		return ref == null ? null : ref.getPlugin("plugin/thumbnail", Thumbnail.class);
	}
}
