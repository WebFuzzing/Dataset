package dev.mirodil.testing_system.configs;

import org.apache.catalina.connector.RequestFacade;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);

        // Convert timestamp to Instant in UTC (second precision)
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        errorAttributes.put("timestamp", timestamp);

        // Convert path URI to full URL path
        RequestFacade requestFacade = ((ServletWebRequest) webRequest).getNativeRequest(RequestFacade.class);
        if (requestFacade != null) {
            String fullUrl = requestFacade.getRequestURL().toString();
            errorAttributes.put("path", fullUrl);
        }

        return errorAttributes;
    }
}