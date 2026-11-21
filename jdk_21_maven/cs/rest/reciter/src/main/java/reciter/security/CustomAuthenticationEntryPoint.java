package reciter.security;

import java.io.IOException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
	
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
    	log.info("CustomAuthenticationEntryPoint triggered with message: " + authException.getMessage());
        if (response.isCommitted()) return;

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String errorMessage = authException.getMessage() != null ? authException.getMessage() : "Unauthorized";

        ErrorResponse error = new ErrorResponse(401, "Unauthorized", errorMessage);
        mapper.writeValue(response.getWriter(), error);
    }
}
