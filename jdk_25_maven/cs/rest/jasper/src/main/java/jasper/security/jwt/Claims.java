package jasper.security.jwt;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Typed accessors for JWT claims.
 */
public class Claims {

	public static final Claims EMPTY = new Claims(Map.of());

	private final Map<String, Object> claims;

	public Claims(Map<String, Object> claims) {
		this.claims = claims;
	}

	public boolean containsKey(String claim) {
		return claims.containsKey(claim);
	}

	public Object get(String claim) {
		return claims.get(claim);
	}

	public <T> T get(String claim, Class<T> type) {
		return type.cast(claims.get(claim));
	}

	public String getString(String claim) {
		return claims.get(claim) instanceof String s ? s : null;
	}

	public Instant getIssuedAt() {
		var iat = claims.get("iat");
		if (iat instanceof Instant i) return i;
		if (iat instanceof Date d) return d.toInstant();
		if (iat instanceof Number n) return Instant.ofEpochSecond(n.longValue());
		return null;
	}
}
