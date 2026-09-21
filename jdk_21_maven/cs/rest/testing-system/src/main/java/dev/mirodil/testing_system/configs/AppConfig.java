package dev.mirodil.testing_system.configs;

import dev.mirodil.testing_system.models.TestEvent;
import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.repositories.GenericRowMapper;
import dev.mirodil.testing_system.repositories.RoleWithPermissionResultSetExtractor;
import dev.mirodil.testing_system.repositories.UserWithPermissionResultSetExtractor;
import dev.mirodil.testing_system.utils.DataUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class AppConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://127.0.0.1:3000",
                "http://localhost:3000",
                "https://testing.mirodil.dev"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public StringRedisTemplate getStringRedisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        return new StringRedisTemplate(lettuceConnectionFactory);
    }

    @Bean
    public UserWithPermissionResultSetExtractor userWithPermissionResultSetExtractor() {
        return new UserWithPermissionResultSetExtractor();
    }

    @Bean
    public RoleWithPermissionResultSetExtractor roleWithPermissionResultSetExtractor() {
        return new RoleWithPermissionResultSetExtractor();
    }

    @Bean
    public RowMapper<User> userRowMapper() {
        return new GenericRowMapper<>(DataUtil::extractUserFromResultSet);
    }

    @Bean
    public RowMapper<TestEvent> testEventRowMapper() {
        return new GenericRowMapper<>(DataUtil::extractTestEventFromResultSet);
    }
}
