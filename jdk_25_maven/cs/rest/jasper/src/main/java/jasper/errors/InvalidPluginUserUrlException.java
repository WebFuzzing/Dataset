package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPluginUserUrlException extends RuntimeException {
	public InvalidPluginUserUrlException(String tag) {
		this(tag, null);
	}
	public InvalidPluginUserUrlException(String tag, Exception e) {
		super("Invalid User Url for plugin " + tag, e);
	}
}
