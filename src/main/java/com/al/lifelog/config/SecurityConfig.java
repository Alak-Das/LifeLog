package com.al.lifelog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        // --- Clinical Roles ---
        @Value("${spring.security.users.physician.password}")
        private String physicianPass;
        @Value("${spring.security.users.nurse.password}")
        private String nursePass;
        @Value("${spring.security.users.pharmacist.password}")
        private String pharmacistPass;
        @Value("${spring.security.users.lab_tech.password}")
        private String labTechPass;

        // --- Admin Roles ---
        @Value("${spring.security.users.registrar.password}")
        private String registrarPass;
        @Value("${spring.security.users.scheduler.password}")
        private String schedulerPass;
        @Value("${spring.security.users.biller.password}")
        private String billerPass;
        @Value("${spring.security.users.practice_mgr.password}")
        private String practiceMgrPass;

        // --- System Roles ---
        @Value("${spring.security.users.sys_admin.password}")
        private String sysAdminPass;
        @Value("${spring.security.users.auditor.password}")
        private String auditorPass;
        @Value("${spring.security.users.integrator.password}")
        private String integratorPass;
        @Value("${spring.security.users.patient.password}")
        private String patientPass;

        @Bean
        public UserDetailsService userDetailsService() {
                List<UserDetails> users = new ArrayList<>();

                // Helper to create user with authorities
                users.add(createUser("physician", physicianPass,
                                "PATIENT_READ", "OBSERVATION_WRITE", "CONDITION_WRITE", "ENCOUNTER_WRITE",
                                "MEDICATION_WRITE",
                                "ALLERGY_WRITE", "IMMUNIZATION_WRITE", "DIAGNOSTIC_READ"));

                users.add(createUser("nurse", nursePass,
                                "PATIENT_READ", "OBSERVATION_WRITE", "CONDITION_READ", "ENCOUNTER_READ",
                                "IMMUNIZATION_WRITE",
                                "MEDICATION_READ"));

                users.add(createUser("pharmacist", pharmacistPass,
                                "PATIENT_READ", "MEDICATION_READ", "ALLERGY_READ"));

                users.add(createUser("lab_tech", labTechPass,
                                "SERVICE_ORDER_READ", "DIAGNOSTIC_WRITE", "OBSERVATION_WRITE"));

                users.add(createUser("registrar", registrarPass,
                                "PATIENT_WRITE", "APPOINTMENT_WRITE", "COVERAGE_WRITE"));

                users.add(createUser("scheduler", schedulerPass,
                                "APPOINTMENT_WRITE", "SCHEDULE_WRITE", "PRACTITIONER_READ"));

                users.add(createUser("biller", billerPass,
                                "ENCOUNTER_READ", "CONDITION_READ", "ACCOUNT_WRITE"));

                users.add(createUser("practice_mgr", practiceMgrPass,
                                "PRACTITIONER_WRITE", "ORGANIZATION_WRITE", "LOCATION_WRITE"));

                users.add(createUser("sys_admin", sysAdminPass,
                                "SUBSCRIPTION_WRITE", "SYSTEM_CONFIG_WRITE"));

                users.add(createUser("auditor", auditorPass,
                                "AUDIT_READ"));

                users.add(createUser("integrator", integratorPass,
                                "OBSERVATION_WRITE"));

                users.add(createUser("patient_user", patientPass,
                                "PATIENT_SELF_READ"));

                return new InMemoryUserDetailsManager(users);
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        private UserDetails createUser(String username, String password, String... authorities) {
                return User.builder()
                                .username(username)
                                .password(passwordEncoder().encode(password))
                                .authorities(authorities)
                                .build();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(authorize -> authorize
                                                // Public Endpoints
                                                .requestMatchers("/.well-known/**", "/fhir/metadata",
                                                                "/actuator/health",
                                                                "/actuator/prometheus")
                                                .permitAll()

                                                // --- Patient Resource ---
                                                // Registrar can Write (Create/Update), Physician/Nurse can Read
                                                .requestMatchers(HttpMethod.POST, "/fhir/Patient")
                                                .hasAuthority("PATIENT_WRITE")
                                                .requestMatchers(HttpMethod.PUT, "/fhir/Patient/**")
                                                .hasAuthority("PATIENT_WRITE")
                                                .requestMatchers(HttpMethod.DELETE, "/fhir/Patient/**")
                                                .hasAuthority("PATIENT_WRITE")
                                                .requestMatchers(HttpMethod.GET, "/fhir/Patient/**")
                                                .hasAnyAuthority("PATIENT_READ", "PATIENT_WRITE", "PATIENT_SELF_READ")

                                                // --- Observation Resource ---
                                                // Physician/Nurse/LabTech/Integrator can Write
                                                .requestMatchers(HttpMethod.POST, "/fhir/Observation")
                                                .hasAnyAuthority("OBSERVATION_WRITE")
                                                .requestMatchers(HttpMethod.GET, "/fhir/Observation/**")
                                                .hasAnyAuthority("OBSERVATION_WRITE", "PATIENT_READ",
                                                                "PATIENT_SELF_READ")

                                                // --- Clinical Resources (General) ---
                                                .requestMatchers("/fhir/Condition/**")
                                                .hasAnyAuthority("CONDITION_WRITE", "CONDITION_READ")
                                                .requestMatchers("/fhir/Encounter/**")
                                                .hasAnyAuthority("ENCOUNTER_WRITE", "ENCOUNTER_READ")
                                                .requestMatchers("/fhir/AllergyIntolerance/**")
                                                .hasAnyAuthority("ALLERGY_WRITE", "ALLERGY_READ")
                                                .requestMatchers("/fhir/Immunization/**")
                                                .hasAnyAuthority("IMMUNIZATION_WRITE", "IMMUNIZATION_READ")
                                                .requestMatchers("/fhir/MedicationRequest/**")
                                                .hasAnyAuthority("MEDICATION_WRITE", "MEDICATION_READ")
                                                .requestMatchers("/fhir/DiagnosticReport/**")
                                                .hasAnyAuthority("DIAGNOSTIC_WRITE", "DIAGNOSTIC_READ")

                                                // --- Administrative Resources ---
                                                .requestMatchers("/fhir/Practitioner/**")
                                                .hasAnyAuthority("PRACTITIONER_WRITE", "PRACTITIONER_READ")
                                                .requestMatchers("/fhir/Organization/**")
                                                .hasAnyAuthority("ORGANIZATION_WRITE")
                                                .requestMatchers("/fhir/Appointment/**")
                                                .hasAnyAuthority("APPOINTMENT_WRITE")

                                                // --- System Resources ---
                                                .requestMatchers("/fhir/Subscription/**")
                                                .hasAuthority("SUBSCRIPTION_WRITE")

                                                // Catch-all
                                                .anyRequest().authenticated())
                                .httpBasic(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable());
                return http.build();
        }
}
