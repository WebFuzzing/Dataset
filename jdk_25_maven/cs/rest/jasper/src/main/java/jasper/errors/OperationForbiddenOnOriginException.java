package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class OperationForbiddenOnOriginException extends RuntimeException {
	public OperationForbiddenOnOriginException(String origin) {
		super("Origin " + origin + " is not whitelisted for this operation.");
	}
}
