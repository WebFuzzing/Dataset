package reciter.security;


import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class APIKeyAuthProvider implements AuthenticationProvider {

    private static final String ADMIN_API_KEY = System.getenv("ADMIN_API_KEY");
    private static final String CONSUMER_API_KEY = System.getenv("CONSUMER_API_KEY");

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // Extract API Key from the Authentication Object
        String apiKey = authentication.getPrincipal().toString();

        if (ADMIN_API_KEY.equals(apiKey)) {
            return new UsernamePasswordAuthenticationToken(apiKey, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        } else if (CONSUMER_API_KEY.equals(apiKey)) {
            return new UsernamePasswordAuthenticationToken(apiKey, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        } else {
            throw new BadCredentialsException("Invalid API Key");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
