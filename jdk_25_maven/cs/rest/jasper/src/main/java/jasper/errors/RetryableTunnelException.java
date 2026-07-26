package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
public class RetryableTunnelException extends Exception {

	public RetryableTunnelException(String msg) {
		super(msg);
	}

	public RetryableTunnelException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
