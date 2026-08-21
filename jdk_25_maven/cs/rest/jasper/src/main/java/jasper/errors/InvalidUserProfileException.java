package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUserProfileException extends RuntimeException {

	public InvalidUserProfileException(String msg) {
		super(msg);
	}
}
