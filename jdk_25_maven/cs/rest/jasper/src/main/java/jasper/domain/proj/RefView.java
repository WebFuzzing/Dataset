package jasper.domain.proj;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;

public interface RefView {
	String getUrl();
	String getOrigin();
	String getTitle();
	String getComment();
	List<String> getTags();
	List<String> getAlternateUrls();
	ObjectNode getPlugins();
	Instant getPublished();
	Instant getCreated();
	Instant getModified();
}
