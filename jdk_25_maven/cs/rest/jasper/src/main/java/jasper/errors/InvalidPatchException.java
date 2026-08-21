package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPatchException extends RuntimeException {

	public InvalidPatchException(String type, Throwable cause) {
		super("Invalid patch for " + type, cause);
	}
}
