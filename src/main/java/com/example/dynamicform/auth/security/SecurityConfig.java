package com.example.dynamicform.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()

                        // Read APIs used by both the user app and the admin app.
                        .requestMatchers(HttpMethod.GET, "/api/forms").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/forms/*").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/forms/*/lottery").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/me/submissions").authenticated()

                        // Admin-only read APIs.
                        .requestMatchers(HttpMethod.GET, "/api/forms/*/submissions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/lotteries/*/winners").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/lotteries/*").hasRole("ADMIN")

                        // User submission write API.
                        .requestMatchers(HttpMethod.POST, "/api/forms/*/submissions").hasRole("USER")

                        // Form and lottery management APIs.
                        .requestMatchers(HttpMethod.POST, "/api/forms/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/forms/*/close").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/forms/*/lotteries").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/lotteries/*/run").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/forms").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/forms/**").hasRole("ADMIN")

                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()

                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**")
                        .permitAll()

                        .requestMatchers(
                                "/actuator/info")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
