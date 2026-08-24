package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPluginException extends RuntimeException {

	public InvalidPluginException(String plugin) {
		this(plugin, null);
	}

	public InvalidPluginException(String plugin, Throwable cause) {
		super("Invalid " + plugin + " plugin.", cause);
	}
}
