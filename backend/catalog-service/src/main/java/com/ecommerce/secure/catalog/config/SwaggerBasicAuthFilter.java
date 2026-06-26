package com.ecommerce.secure.catalog.config;

import com.ecommerce.secure.catalog.entity.User;
import com.ecommerce.secure.catalog.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

@Component
public class SwaggerBasicAuthFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/swagger-ui") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Basic ")) {
                String base64Credentials = authHeader.substring("Basic ".length()).trim();
                try {
                    byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
                    String credentials = new String(credDecoded, StandardCharsets.UTF_8);
                    final String[] values = credentials.split(":", 2);

                    if (values.length == 2) {
                        String username = values[0];
                        String password = values[1];

                        Optional<User> userOpt = userRepository.findByUsername(username);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            if (user.getRole() != null && user.getRole().equals("ROLE_ADMIN") && (user.getIsActive() == null || user.getIsActive())) {
                                MessageDigest digest = MessageDigest.getInstance("SHA3-512");
                                byte[] hashedBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
                                String computedHash = java.util.HexFormat.of().formatHex(hashedBytes);

                                if (computedHash.equalsIgnoreCase(user.getPasswordHash())) {
                                    filterChain.doFilter(request, response);
                                    return;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore decode or hash errors, just fallback to unauthorized
                }
            }

            response.setHeader("WWW-Authenticate", "Basic realm=\"Secure API Docs (Admin Only)\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized: Admin credentials required.");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
