package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PublishDateException extends RuntimeException {

	public PublishDateException(String responseUrl, String sourceUrl) {
		super("Source " + sourceUrl + " must predate response " + responseUrl);
	}
}
