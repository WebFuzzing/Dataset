package jasper.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UntrustedScriptException extends Exception {
	private final String scriptHash;

	public UntrustedScriptException(String scriptHash) {
		this.scriptHash = scriptHash;
	}

	public String getScriptHash() {
		return scriptHash;
	}
}
