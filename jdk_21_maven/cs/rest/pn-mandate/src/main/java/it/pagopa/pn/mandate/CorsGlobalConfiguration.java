package it.pagopa.pn.mandate;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Configuration
@lombok.CustomLog
public class CorsGlobalConfiguration implements WebFluxConfigurer {

    @Value("${cors.allowed.domains:}")
    private List<String> corsAllowedDomains;

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {

        if (log.isInfoEnabled())
            log.info("allowed domains:" + String.join(", ", corsAllowedDomains));

        corsRegistry.addMapping("/**")
                .allowedOrigins( corsAllowedDomains.toArray( new String[0] ) )
                .allowedMethods("GET", "HEAD", "OPTIONS", "POST", "PUT", "DELETE", "PATCH")
                .maxAge(3600);
    }
}