package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FreshLoginException extends RuntimeException {
	public FreshLoginException() {
		super("Requires reauthorization. Please log again to access.");
	}
}
