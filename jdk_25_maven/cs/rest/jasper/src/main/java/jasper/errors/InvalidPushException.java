package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPushException extends RuntimeException {
	public InvalidPushException() {
		super("Push contains invalid data.");
	}
}
