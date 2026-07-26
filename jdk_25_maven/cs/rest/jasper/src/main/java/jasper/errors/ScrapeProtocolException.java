package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ScrapeProtocolException extends RuntimeException {

	public ScrapeProtocolException(String protocol) {
		super("Cannot scrape protocol: " + protocol);
	}
}
