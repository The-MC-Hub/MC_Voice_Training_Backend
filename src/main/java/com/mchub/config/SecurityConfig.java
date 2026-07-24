package com.mchub.config;

import com.mchub.repositories.UserRepository;
import com.mchub.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${mchub.cors.allowed-origins:http://localhost:5173}")
    private String allowedOriginsRaw;

    // Comma-separated Vercel *preview* domain patterns for this project only, e.g.
    // "https://mc-voice-training-*.vercel.app". Left empty by default — do NOT set this to
    // "https://*.vercel.app", which would trust every Vercel project on the platform.
    @Value("${mchub.cors.vercel-preview-patterns:}")
    private String vercelPreviewPatternsRaw;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .xssProtection(xss -> xss.disable()) // modern browsers use CSP instead
                        .contentTypeOptions(ct -> {})        // X-Content-Type-Options: nosniff
                        .frameOptions(frame -> frame.deny()) // X-Frame-Options: DENY (clickjacking)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/scripts/**").permitAll()
                        .requestMatchers("/api/v1/payment/webhook").permitAll()
                        .requestMatchers("/api/v1/payment/booking/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payment/status/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payment/plans").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payment/flash-deals").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payment/apply-discount").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/reading-guides/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/voice/lessons/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/voice/lessons").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/voice/practice/analyze-guest").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/voice/guest-cooldown-hours").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/community/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/mc/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/availability/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/certificates/mc/**").permitAll()
                        .requestMatchers("/ws-chat/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/social-posts").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/social-posts/*/click").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/minigames/speed-reader/prompts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/minigames/leaderboard").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(new JwtAuthenticationFilter(Objects.requireNonNull(jwtService), userRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = new ArrayList<>(Arrays.asList(allowedOriginsRaw.split(",")));
        if (vercelPreviewPatternsRaw != null && !vercelPreviewPatternsRaw.isBlank()) {
            for (String pattern : vercelPreviewPatternsRaw.split(",")) {
                String trimmed = pattern.trim();
                if (!trimmed.isBlank()) origins.add(trimmed);
            }
        }
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
