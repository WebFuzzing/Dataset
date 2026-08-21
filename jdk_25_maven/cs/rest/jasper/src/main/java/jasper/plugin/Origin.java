package jasper.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jasper.domain.proj.HasTags;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

import static jasper.domain.proj.HasTags.getPlugin;


@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class Origin implements Serializable {
	private String local = "";
	private String remote = "";
	private String proxy;

	private static final Origin DEFAULTS = new Origin();
	public static Origin getOrigin(HasTags ref) {
		var origin = ref == null ? null : getPlugin(ref, "+plugin/origin", Origin.class);
		return origin == null ? DEFAULTS : origin;
	}
}
