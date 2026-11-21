package reciter.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author mjangari
 * Validates the api-key received from the reciter-consumer and reciter respectively. 
 */
@Component
public class MultiApiKeyFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(MultiApiKeyFilter.class);
	
	private String adminPrincipalRequestValue = System.getenv("ADMIN_API_KEY");
	    
	private String consumerPrincipalRequestValue = System.getenv("CONSUMER_API_KEY");
		
	@Value("${spring.security.enabled}")
	private boolean securityEnabled;

	@Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException 
	{

		
		if (!securityEnabled) {
            // Security off: just continue
            filterChain.doFilter(request, response);
            return;
        }
		
        String path = request.getRequestURI();
        try
		{
	        /*
	         * Fall back for consumer users until migration to JWT tokens is complete.
	         * TODO: Once all existing users have been migrated, this conditional code will be removed.
	         */
	        if(((path.startsWith("/reciter/article-retrieval/") || path.startsWith("/reciter/dev/article-retrieval/"))))
			{
				Optional.ofNullable(request.getHeader("api-key"))
		        .filter(key -> !key.isBlank())
		        .ifPresent(apiKey -> {
		        	 if (!constantTimeEquals(apiKey, consumerPrincipalRequestValue)) {
		        		 throw new BadCredentialsException("Invalid API key");
		                }
		                UsernamePasswordAuthenticationToken auth =
		                        new UsernamePasswordAuthenticationToken("reciter-consumer-user", null, Collections.emptyList());
		                SecurityContextHolder.getContext().setAuthentication(auth);
	
		        });
			}
			else if(path.startsWith("/reciter/"))
			{
				Optional.ofNullable(request.getHeader("api-key"))
		        .filter(key -> !key.isBlank())
		        .ifPresent(apiKey -> {
		        	 if (!constantTimeEquals(apiKey, adminPrincipalRequestValue)) {
		        		 throw new BadCredentialsException("Invalid API key");
		                }
		                UsernamePasswordAuthenticationToken auth =
		                        new UsernamePasswordAuthenticationToken("reciter-internal-user", null, Collections.emptyList());
		                SecurityContextHolder.getContext().setAuthentication(auth);
	
		        });
			}
		}
	    catch (AuthenticationException ex) {
			authenticationEntryPoint.commence(request, response, ex);
			return;
	    }

		filterChain.doFilter(request, response);
		
	}
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
	    String path = request.getServletPath();
	    String authHeader = request.getHeader("Authorization");

	    // 1. Skip if it's the login path
	    if (path.equals("/reciter/generate-access-token")) {
	        return true;
	    }

	    // 2. Skip if it's a JWT-based request
	    // If the header has "Bearer", let JwtDecoder handle it later in the chain
	    if (authHeader != null && authHeader.startsWith("Bearer ")) {
	        return true;
	    }

	    return false;
	}

	/**
	 * Constant-time comparison of the supplied api-key against the expected value,
	 * to avoid leaking the key through response-timing side channels (String.equals
	 * short-circuits on the first differing byte). Returns false (rather than
	 * throwing) when either value is null/absent, preserving the prior reject-on-
	 * mismatch behavior.
	 */
	static boolean constantTimeEquals(String provided, String expected) {
		if (provided == null || expected == null) {
			return false;
		}
		return MessageDigest.isEqual(
				provided.getBytes(StandardCharsets.UTF_8),
				expected.getBytes(StandardCharsets.UTF_8));
	}
}
