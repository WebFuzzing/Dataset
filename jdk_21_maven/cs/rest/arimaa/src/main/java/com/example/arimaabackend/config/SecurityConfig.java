package com.example.arimaabackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.arimaabackend.security.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        try {
            return config.getAuthenticationManager();
        } catch (Exception e) {
            throw new RuntimeException("Could not get AuthenticationManager bean", e);
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        String localAdmin = "ADMIN";
        try {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    // Admin-only: create, get, delete users
                    .requestMatchers(HttpMethod.POST, "/api/users").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.POST, "/api/mongo/users").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.DELETE, "/api/mongo/users/**").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.POST, "/api/neo4j/users").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.DELETE, "/api/neo4j/users/**").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.POST, "/api/mongo/players").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.PUT, "/api/mongo/players/**").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.DELETE, "/api/mongo/players/**").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.POST, "/api/mongo/matches").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.PUT, "/api/mongo/matches/**").hasRole(localAdmin)
                    .requestMatchers(HttpMethod.DELETE, "/api/mongo/matches/**").hasRole(localAdmin)
                    .requestMatchers("/api/admin/**").hasRole(localAdmin)
                    // Everything else requires authentication
                    .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

            return http.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build SecurityFilterChain", e);
        }
    }

}
