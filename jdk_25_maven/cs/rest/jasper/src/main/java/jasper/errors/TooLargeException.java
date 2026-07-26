package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
public class TooLargeException extends RuntimeException {

	public TooLargeException(int requested, int max) {
		super("You requested " + requested + " entities, but the max is " + max + ".");
	}
}
