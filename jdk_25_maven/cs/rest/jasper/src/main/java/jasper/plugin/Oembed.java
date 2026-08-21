package jasper.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;


@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class Oembed implements Serializable {
	private String provider_name;
	private String provider_url;
	private List<Endpoints> endpoints;

	@Getter
	@Setter
	@JsonInclude(Include.NON_NULL)
	public static class Endpoints implements Serializable {
		private List<String> schemes;
		private List<String> formats;
		private String url;
		private boolean discovery;
	}

}
