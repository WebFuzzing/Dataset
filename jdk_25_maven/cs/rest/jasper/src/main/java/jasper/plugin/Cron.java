package jasper.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jasper.domain.Ref;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Duration;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class Cron implements Serializable {
	private Duration interval = Duration.ofMinutes(15);

	private static final Cron DEFAULTS = new Cron();
	public static Cron getCron(Ref ref) {
		var cron = ref == null ? null : ref.getPlugin("plugin/cron", Cron.class);
		return cron == null ? DEFAULTS : cron;
	}
}
