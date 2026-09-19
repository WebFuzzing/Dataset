package org.movies.xml.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Content negotiation hardening: XML is the default representation and a query
 * parameter can never ask for anything else.
 *
 * <p>The message converters are left alone on purpose. Removing the JSON converter
 * would also break the OpenAPI schema that springdoc serves at /v3/api-docs, which
 * has to stay JSON; the explicit consumes/produces on every endpoint already keep
 * the API itself XML-only.
 *
 * <p>For the same reason JSON trails XML in the default content types: a client that
 * states no preference gets XML from every API endpoint, while /v3/api-docs, whose
 * only producible type is JSON, still answers instead of failing with 406.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON);
    }
}
