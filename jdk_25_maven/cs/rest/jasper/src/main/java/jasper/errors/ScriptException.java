package jasper.errors;

public class ScriptException extends Exception {
	private final String logs;

	public ScriptException(String message, String logs) {
		super(message);
		this.logs = logs;
	}

	public String getLogs() {
		return logs;
	}
}
