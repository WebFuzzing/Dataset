package jasper.security.jwt;

import org.springframework.security.core.Authentication;

public interface TokenProvider {
	boolean validateToken(String jwt, String origin);
	Authentication getAuthentication(String jwt, String origin);
}
