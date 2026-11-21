package reciter.security;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class S3LoggingFilter extends OncePerRequestFilter {

	private final S3UserLogHandler s3UserLogHandler;
	
    public S3LoggingFilter(S3UserLogHandler s3UserLogHandler) {
        this.s3UserLogHandler = s3UserLogHandler;
    }

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException 
	{
		
	        // Execute the security chain first
	        filterChain.doFilter(request, response);
	        
	     // 2. Now the response has a status code (e.g., 200, 401, 500)
	        int statusCode = response.getStatus();

	        // After the request is processed, check if we have a valid JWT
	        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

	        if (auth != null && auth.getPrincipal() instanceof Jwt) {
	            Jwt jwt = (Jwt) auth.getPrincipal();
	            
	            // Extract metadata from JWT and Request
	            String clientId = jwt.getClaimAsString("client_id");
	            String clientName = jwt.getClaimAsString("client_name"); // Ensure this claim exists
	            String uri = request.getRequestURI();
	            String uid = request.getParameter("uid");
	            String timestamp = LocalDateTime.now().toString();
	            String dateFolder = LocalDate.now().toString();

	            
	            // Create log object
	            UserLog userLog = new UserLog(clientId, clientName, uri, uid, timestamp,statusCode);

	            // FIRE AND FORGET: This call is @Async, so it doesn't block the response
	            s3UserLogHandler.writeUserLog(userLog, dateFolder);
	        }
	    }
}
