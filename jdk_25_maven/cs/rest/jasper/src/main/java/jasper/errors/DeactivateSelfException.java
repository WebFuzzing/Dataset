package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DeactivateSelfException extends RuntimeException {
	public DeactivateSelfException() {
		super("You cannot deactivate your own account.");
	}
}
