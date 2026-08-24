package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateModifiedDateException extends RuntimeException {
	public DuplicateModifiedDateException() {
		super("Modified date must be unique.");
	}
}
