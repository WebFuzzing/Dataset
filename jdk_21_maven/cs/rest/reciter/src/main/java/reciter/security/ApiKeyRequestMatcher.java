package reciter.security;



import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

public class ApiKeyRequestMatcher implements RequestMatcher {

	@Override
    public boolean matches(HttpServletRequest request) {
        return request.getHeader("api-key") != null;
    }

}
