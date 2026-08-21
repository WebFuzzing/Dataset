package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTunnelException extends RuntimeException {

	public InvalidTunnelException(String msg) {
		super(msg);
	}

	public InvalidTunnelException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
