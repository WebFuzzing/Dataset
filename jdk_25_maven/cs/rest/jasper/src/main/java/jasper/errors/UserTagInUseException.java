package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserTagInUseException extends RuntimeException {
	public UserTagInUseException() {
		super("User tag already in use by another user.");
	}
}
