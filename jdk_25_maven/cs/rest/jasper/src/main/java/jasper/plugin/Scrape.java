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
public class Scrape implements Serializable {
	private List<String> schemes;
	private boolean text;
	private boolean oembedJson;
	private boolean ldJson;
	private boolean openGraph;
	private List<String> textSelectors;
	private List<String> publishedSelectors;
	private List<String> removeSelectors;
	private List<String> removeAfterSelectors;
	private List<String> removeStyleSelectors;
	private List<String> imageFixRegex;
	private List<String> imageSelectors;
	private List<String> videoSelectors;
	private List<String> audioSelectors;
	private List<String> thumbnailSelectors;
}
