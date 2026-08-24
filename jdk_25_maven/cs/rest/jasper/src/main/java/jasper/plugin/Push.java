package jasper.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jasper.domain.Ref;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class Push implements Serializable {
	private boolean pushOnChange;
	private boolean cache;
	private String query;
	private int batchSize;

	private static final Push DEFAULTS = new Push();
	public static Push getPush(Ref ref) {
		var push = ref == null ? null : ref.getPlugin("+plugin/origin/push", Push.class);
		return push == null ? DEFAULTS : push;
	}
}
