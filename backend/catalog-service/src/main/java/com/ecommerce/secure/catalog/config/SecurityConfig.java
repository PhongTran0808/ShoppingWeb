package com.ecommerce.secure.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // Public read access to catalog products and categories
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                .requestMatchers("/api/users/login", "/api/users/register").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Write/Delete operations on products require Admin scope/role
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyAuthority("SCOPE_admin", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyAuthority("SCOPE_admin", "ROLE_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyAuthority("SCOPE_admin", "ROLE_ADMIN")
                
                // All user management APIs require Admin role/scope
                .requestMatchers("/api/users/**").hasAnyAuthority("SCOPE_admin", "ROLE_ADMIN")
                
                // Fallback: all other requests need authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        return http.build();
    }
}
