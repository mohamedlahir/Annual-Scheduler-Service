package com.laby.annual.scheduler.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JWTFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    // allow health and actuator
                    auth.requestMatchers("/actuator/**").permitAll()
                            // allow Spring error endpoint so error dispatches don't get blocked
                            .requestMatchers("/error").permitAll()
                            // allow debug endpoints (used during development)
                            .requestMatchers("/api/scheduler/debug/**").permitAll()
                            // protect scheduler APIs - require role ADMIN, STUDENT or TUTOR
                            .requestMatchers("/api/scheduler/**").hasAnyRole("ADMIN","STUDENT","TUTOR")
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
