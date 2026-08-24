package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PullLocalException extends RuntimeException {
	public PullLocalException(String origin) {
		super("Can't pull into local origin (" + origin + "). You must pull into a nested origin.");
	}
}
