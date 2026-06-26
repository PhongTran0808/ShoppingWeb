package com.ecommerce.secure.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
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
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                // === OPTIONS for CORS Preflight ===
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // === Swagger UI & OpenAPI docs - SECURED BY SwaggerBasicAuthFilter ===
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

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

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("VaultCommerce API - Catalog Service")
                .version("1.0.0")
                .description("""
                    ## 🔐 Hệ thống E-Commerce Mật mã ứng dụng
                    
                    Backend API của nền tảng VaultCommerce, cung cấp các endpoint quản lý:
                    - **Sản phẩm** (Products): CRUD, tìm kiếm, lọc theo danh mục
                    - **Người dùng** (Users): Đăng ký, Đăng nhập, Quản trị
                    - **Danh mục** (Categories): Phân loại sản phẩm
                    
                    Tất cả dữ liệu nhạy cảm được bảo vệ bằng **AES-256 GCM** thông qua HashiCorp Vault.
                    """)
                .contact(new Contact()
                    .name("VaultCommerce Team")
                    .email("admin@tienthienvienman.site.je"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Nhập JWT Token lấy từ endpoint /api/users/login")));
    }
}
