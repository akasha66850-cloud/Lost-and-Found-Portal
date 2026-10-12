package com.example.lostfound.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenUtil jwtUtil;

    public SecurityConfig(JwtTokenUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtUtil);

        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // ── Public HTML pages ───────────────────────────────
                .requestMatchers(
                    "/", "/index.html",
                    "/login.html", "/register.html",
                    "/item.html", "/post.html",
                    "/report-lost.html", "/report-found.html",
                    "/my-claims.html", "/my-reports.html",
                    "/admin-claims.html",
                    "/admin.html",
                    "/notifications.html",
                    "/search.html",
                    "/dashboard.html"
                ).permitAll()

                // ── Static assets ───────────────────────────────────
                .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll()

                // ── Public APIs ──────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/items/**").permitAll()
                .requestMatchers("/api/dashboard/**").permitAll()

                // ── Admin-only APIs ──────────────────────────────────
                .requestMatchers(
                    "/api/claims/pending",
                    "/api/claims/all",
                    "/api/admin/**"
                ).hasRole("ADMIN")

                // ── Everything else requires authentication ───────────
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}