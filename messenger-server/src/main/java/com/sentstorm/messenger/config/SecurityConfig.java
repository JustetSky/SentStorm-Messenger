package com.sentstorm.messenger.config;

import com.sentstorm.messenger.security.filter.UserSynchronizerFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserSynchronizerFilter userSynchronizerFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.setAllowedOrigins(Arrays.asList(
                            "http://localhost:4200"
                    ));
                    config.setAllowedHeaders(Arrays.asList("*"));
                    config.setAllowedMethods(Arrays.asList("*"));
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req ->
                        req
                                // swagger
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html"
                                ).permitAll()
                                .requestMatchers("/ws/**").permitAll()
                                // Изображения требуют аутентификации
                                .requestMatchers("/uploads/**").authenticated()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter())
                        )
                )
                // Добавляем фильтр для обработки токена из query параметра
                .addFilterBefore(new TokenFromQueryFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(
                        userSynchronizerFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    private static class TokenFromQueryFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                jakarta.servlet.http.HttpServletRequest request,
                jakarta.servlet.http.HttpServletResponse response,
                jakarta.servlet.FilterChain filterChain
        ) throws java.io.IOException, jakarta.servlet.ServletException {

            String token = request.getParameter("token");

            if (token != null && !token.isEmpty() && request.getHeader("Authorization") == null) {
                // Создаём обёртку запроса с добавленным заголовком Authorization
                request = new jakarta.servlet.http.HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("Authorization".equalsIgnoreCase(name)) {
                            return "Bearer " + token;
                        }
                        return super.getHeader(name);
                    }
                };
            }

            filterChain.doFilter(request, response);
        }
    }
}