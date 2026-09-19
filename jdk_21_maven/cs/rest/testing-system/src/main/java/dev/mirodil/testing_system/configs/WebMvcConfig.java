package dev.mirodil.testing_system.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final PageableWithFilterResolver pageableWithFilterResolver;

    public WebMvcConfig(PageableWithFilterResolver pageableWithFilterResolver) {
        this.pageableWithFilterResolver = pageableWithFilterResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(pageableWithFilterResolver);
    }
}