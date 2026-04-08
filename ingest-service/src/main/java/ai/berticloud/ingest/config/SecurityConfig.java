package ai.berticloud.ingest.config;

import ai.berticloud.ingest.auth.MtlsHeaderFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MtlsHeaderFilter mtlsHeaderFilter;

    public SecurityConfig(MtlsHeaderFilter mtlsHeaderFilter) {
        this.mtlsHeaderFilter = mtlsHeaderFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Permettiamo l'accesso a tutti perché l'IngestController gestisce i token non validati
                .anyRequest().permitAll()
            )
            .addFilterBefore(mtlsHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
