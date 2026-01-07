package com.al.lifelog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.admin.username}")
    private String adminUsername;

    @Value("${spring.security.admin.password}")
    private String adminPassword;

    @Value("${spring.security.clinical.username}")
    private String clinicalUsername;

    @Value("${spring.security.clinical.password}")
    private String clinicalPassword;

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password("{noop}" + adminPassword)
                .roles("ADMIN")
                .build();

        UserDetails clinician = User.builder()
                .username(clinicalUsername)
                .password("{noop}" + clinicalPassword)
                .roles("CLINICAL")
                .build();

        return new InMemoryUserDetailsManager(admin, clinician);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Public Endpoints
                        .requestMatchers("/.well-known/**", "/fhir/metadata", "/actuator/health").permitAll()

                        // Admin Resources
                        .requestMatchers("/fhir/Patient/**", "/fhir/Practitioner/**", "/fhir/Organization/**",
                                "/fhir/Appointment/**")
                        .hasRole("ADMIN")

                        // Clinical Resources
                        .requestMatchers("/fhir/Observation/**", "/fhir/Condition/**", "/fhir/Encounter/**",
                                "/fhir/AllergyIntolerance/**", "/fhir/Immunization/**", "/fhir/MedicationRequest/**",
                                "/fhir/DiagnosticReport/**")
                        .hasRole("CLINICAL")

                        // Catch-all (e.g. Subscription, System) - Require Auth (Admin or Clinical?)
                        // Subscription is often system-level, let's say Admin.
                        .requestMatchers("/fhir/Subscription/**").hasRole("ADMIN")

                        // Require authentication for anything else
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
