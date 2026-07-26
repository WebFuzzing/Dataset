package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTemplateException extends RuntimeException {

	public InvalidTemplateException(String template) {
		this(template, null);
	}

	public InvalidTemplateException(String template, Throwable cause) {
		super("Invalid " + template + " template.", cause);
	}
}
