package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MaxSourcesException extends RuntimeException {

	public MaxSourcesException(int max, int count) {
		super("Max count is set to " + max + ". Ref contains " + count + " sources.");
	}
}
