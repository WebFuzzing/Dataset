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
public class Tunnel implements Serializable {
	private String hostFingerprint;
	private String remoteUser;
	private String sshHost;
	private int sshPort = 8022;

	private static final Tunnel DEFAULTS = new Tunnel();
	public static Tunnel getTunnel(HasTags ref) {
		var tunnel = ref == null ? null : getPlugin(ref, "+plugin/origin/tunnel", Tunnel.class);
		return tunnel == null ? DEFAULTS : tunnel;
	}
}
